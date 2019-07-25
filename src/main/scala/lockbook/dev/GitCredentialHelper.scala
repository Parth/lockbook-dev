package lockbook.dev

import java.io.{File, PrintWriter}

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
  def getCredentials(key: String): Try[GitCredential]

  /**
    * Provides a way for a caller to reject credentials provided and re-prompt the user for credentials
    *
    * @param key key for incorrect credentials
    * @return The GitCredential the user re-entered
    */
  def incorrectCredentials(key: String): Unit
}

class GitCredentialHelperImpl(
    encryptionHelper: EncryptionHelper,
    passwordHelper: PasswordHelper,
    fileHelper: FileHelper
) extends GitCredentialHelper {

  val credentialFolder = s"${App.path}/credentials"

  override def getCredentials(key: String): Try[GitCredential] = {
    fileHelper
      .readFile(s"$credentialFolder/$key")
      .map(EncryptedValue) match {
      case Failure(_) =>
        val toRet = GitCredentialUi.getView(key).showAndWait().get()

        toRet
          .map(GitCredential.deserialize)
          .map(DecryptedValue)
          .flatMap(encryptionHelper.encrypt(_, passwordHelper.password))
          .flatMap(saveToFile(_, key))

        toRet
      case Success(value) =>
        Success(value)
          .flatMap(encryptionHelper.decrypt(_, passwordHelper.password))
          .map(_.secret)
          .flatMap(GitCredential.serialize)
    }
  }

  private def saveToFile(encryptedValue: EncryptedValue, key: String): Try[Unit] = {
    try {
      val file = new File(s"$credentialFolder/$key")
      file.getParentFile.mkdirs
      file.createNewFile

      val pw = new PrintWriter(file)
      pw.write(encryptedValue.garbage)
      pw.close()

      if (pw.checkError()) {
        Failure(new Error("Something went wrong while saving this credential"))
      } else {
        Success()
      }
    } catch {
      case _: SecurityException =>
        Failure(new Error(s"I do not have write access to $credentialFolder/$key"))
    }
  }

  override def incorrectCredentials(key: String): Unit = {
    new File(s"$credentialFolder/$key").delete()
  }
}
