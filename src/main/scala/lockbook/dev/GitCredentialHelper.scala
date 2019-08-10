package lockbook.dev

import java.io.File
import java.nio.file.NoSuchFileException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class GitCredential(username: String, password: String)
object GitCredential { // Apply Unapply instead of this
  def deserialize(gitCredential: GitCredential): String =
    s"${gitCredential.username},${gitCredential.password}"

  def serialize(string: String): Try[GitCredential] = {
    val parts = string.split(",")
    if (parts.length == 2) {
      Success(GitCredential(parts.head, parts.last))
    } else {
      Failure(new Error("Failed to read stored credentials."))
    }
  }
}

trait GitCredentialHelper {

  /**
    * @param key retrieve a GitCredential. Will see if there's an encrypted credential file,
    *            decrypt it and return the stored GitCredential. If such a file does not exist
    *            it will prompt the user for one.
    * @return last entered GitCredential for a given file
    */
  def getCredentials(key: String): Future[Option[GitCredential]]

  /**
    * Provides a way for a caller to reject credentials provided and re-prompt the user for credentials
    *
    * @param key key for incorrect credentials
    * @return The GitCredential the user re-entered
    */
  def deleteStoredCredentials(key: String): Future[Unit]
}

class GitCredentialHelperImpl(
    encryptionHelper: EncryptionHelper,
    passwordHelper: PasswordHelper,
    fileHelper: FileHelper
) extends GitCredentialHelper {

  val credentialFolder = s"${App.path}/credentials"

  override def getCredentials(key: String): Future[Option[GitCredential]] = {
    val maybePassword = getPasswordFromFileOrUi(key)

    maybePassword foreach {_ foreach savePassword(key)}

    maybePassword
  }

  private def getPasswordFromFileOrUi(key: String): Future[Option[GitCredential]] = {
    readPasswordFileFor(key).map(result => Some(result)) recover {
      case _: WrongPassword =>
        deleteStoredCredentials(key)
        GitCredentialUi.getView(key).showAndWait().get()
      case _: NoSuchFileException =>
        GitCredentialUi.getView(key).showAndWait().get()
    }
  }

  private def readPasswordFileFor(key: String): Future[GitCredential] = {
    fileHelper
      .readFile(s"$credentialFolder/$key")
      .map(EncryptedValue)
      .flatMap(value => Future.fromTry(encryptionHelper.decrypt(value, passwordHelper.password)))
      .flatMap(
        decryptedCredential => Future.fromTry(GitCredential.serialize(decryptedCredential.secret))
      )
  }

  private def savePassword(key: String)(gitCredential: GitCredential) = Future {
    val decryptedValue = DecryptedValue(secret = GitCredential.deserialize(gitCredential))
    val encryptedValue = encryptionHelper.encrypt(decryptedValue, passwordHelper.password)
    fileHelper.saveToFile(new File(s"$credentialFolder/key"), encryptedValue.get.garbage)
  }

  override def deleteStoredCredentials(key: String): Future[Unit] = Future {
    new File(s"$credentialFolder/$key").delete()
  }
}
