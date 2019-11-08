package lockbook.dev

import java.io.File

import javafx.application.Platform

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

case class GitCredential(username: String, password: String)
object GitCredential { // TODO Apply Unapply instead of this
  def serialize(gitCredential: GitCredential): String =
    s"${gitCredential.username},${gitCredential.password}"

  def deserialize(string: String): Either[GitError, GitCredential] = {
    val parts = string.split(",")
    if (parts.length == 2) {
      Right(GitCredential(parts.head, parts.last))
    } else {
      Left(CouldNotStoreCredentials())
    }
  }
}

trait GitCredentialHelper { // TODO consider splitting this out into ui based & file based

  /**
    * @param key retrieve a GitCredential. Will see if there's an encrypted credential file,
    *            decrypt it and return the stored GitCredential. If such a file does not exist
    *            it will prompt the user for one.
    * @param askUser should the user be prompted for a credential? Useful to disable during polling events
    * @return last entered GitCredential for a given file
    */
  def getCredentials(key: String, askUser: Boolean): Either[LockbookError, GitCredential]

  /**
    * Provides a way for a caller to reject credentials provided and re-prompt the user for credentials
    *
    * @param key key for incorrect credentials
    * @return The GitCredential the user re-entered
    */
  def deleteStoredCredentials(key: String): Either[FileError, Unit]
}

class GitCredentialHelperImpl(
    encryptionHelper: EncryptionHelper,
    passwordHelper: PassphraseHelper,
    fileHelper: FileHelper
) extends GitCredentialHelper {

  val credentialFolder = s"${App.path}/credentials"

  override def getCredentials(key: String, askUser: Boolean): Either[LockbookError, GitCredential] = {
    getPasswordFromFile(key) match {
      case Left(error) =>
        println(s"Could not read password file: $error")
        if (askUser)
          getPasswordFromUser(key) // In this branch password error is ignored and logged, we hope to recover by asking the user
        else
          Left(error)
      case Right(value) =>
        Right(value)
    }
  }

  private def getPasswordFromUser(key: String): Either[UserCanceled, GitCredential] = {
    deleteStoredCredentials(key) // If decryption failed on this password, delete it

    // This is required because, this function is not called from JavaFX thread, so we need to Platform.runLater, which is async
    val p: Promise[Either[UserCanceled, GitCredential]] =
      Promise[Either[UserCanceled, GitCredential]]()

    Platform.runLater(() => {
      p.success(GitCredentialUi.getView(key).showAndWait().get())
    })

    Await.result(p.future, Duration.Inf) match {
      case Left(value) =>
        Left(value)
      case Right(value) =>
        save(key, value)
        Right(value)
    }
  }

  private def getPasswordFromFile(key: String): Either[LockbookError, GitCredential] = {
    fileHelper
      .readFile(s"$credentialFolder/$key")
      .map(EncryptedValue)
      .flatMap(encryptionHelper.decrypt(_, passwordHelper.passphrase))
      .map(_.secret)
      .flatMap(GitCredential.deserialize)
  }

  private def save(key: String, gitCredential: GitCredential): Either[LockbookError, Unit] = {
    val decryptedValue = DecryptedValue(secret = GitCredential.serialize(gitCredential))

    encryptionHelper
      .encrypt(decryptedValue, passwordHelper.passphrase)
      .map(_.garbage)
      .flatMap(fileHelper.saveToFile(new File(s"$credentialFolder/$key"), _))
  }

  override def deleteStoredCredentials(key: String): Either[FileError, Unit] = {
    val file = new File(s"$credentialFolder/$key")
    try {
      if (file.delete()) {
        Right(())
      } else {
        Left(UnableToDeleteFile(file))
      }
    } catch {
      case sec: SecurityException => Left(UnableToAccessFile(file, sec))
    }
  }
}
