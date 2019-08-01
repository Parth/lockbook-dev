package lockbook.dev

import java.io.{File, PrintWriter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class GitCredential(username: String, password: String)
object GitCredential {
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
  def getCredentials(key: String): Future[GitCredential]

  /**
    * Provides a way for a caller to reject credentials provided and re-prompt the user for credentials
    *
    * @param key key for incorrect credentials
    * @return The GitCredential the user re-entered
    */
  def incorrectCredentials(key: String): Future[Unit]
}

class GitCredentialHelperImpl(
    encryptionHelper: EncryptionHelper,
    passwordHelper: PasswordHelper,
    fileHelper: FileHelper
) extends GitCredentialHelper {

  val credentialFolder = s"${App.path}/credentials"

  override def getCredentials(key: String): Future[GitCredential] = {
    readPasswordFileFor(key) recover {

    }
     onComplete {
      case Failure(_) =>
        val toRet = GitCredentialUi.getView(key).showAndWait().get()

        toRet
          .map(GitCredential.deserialize)
          .map(DecryptedValue)
          .flatMap(encryptionHelper.encrypt(_, passwordHelper.password))
          .foreach(saveToFile(_, key))

        toRet
      case Success(value) =>
        value)
          .flatMap(encryptionHelper.decrypt(_, passwordHelper.password))
          .map(_.secret)
          .flatMap(GitCredential.serialize)
    }
  }

  private def readPasswordFileFor(key: String): Future[EncryptedValue] =
    fileHelper
      .readFile(s"$credentialFolder/$key")
      .map(EncryptedValue)

  private def saveToFile(encryptedValue: EncryptedValue, key: String): Future[Unit] = {
    Future {
      val file = new File(s"$credentialFolder/$key")
      file.getParentFile.mkdirs
      file.createNewFile

      val pw = new PrintWriter(file)
      pw.write(encryptedValue.garbage)
      pw.close()
      pw
    } onComplete {
      case Success(pw) =>
        if (pw.checkError())
          Future.failed(new Error("Something went wrong while saving this credential"))
        else
          Future.successful()
      case Failure(_) =>
        Future.failed(new Error(s"I do not have write access to $credentialFolder/$key"))
    }
  }

  override def incorrectCredentials(key: String): Future[Unit] = Future {
    new File(s"$credentialFolder/$key").delete()
  }
}
