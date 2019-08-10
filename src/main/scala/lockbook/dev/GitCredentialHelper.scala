package lockbook.dev

import java.io.File

case class GitCredential(username: String, password: String)
object GitCredential { // Apply Unapply instead of this
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

trait GitCredentialHelper {

  /**
    * @param key retrieve a GitCredential. Will see if there's an encrypted credential file,
    *            decrypt it and return the stored GitCredential. If such a file does not exist
    *            it will prompt the user for one.
    * @return last entered GitCredential for a given file
    */
  def getCredentials(key: String): Either[UserCanceled, GitCredential]

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
    passwordHelper: PasswordHelper,
    fileHelper: FileHelper
) extends GitCredentialHelper {

  val credentialFolder = s"${App.path}/credentials"

  override def getCredentials(key: String): Either[UserCanceled, GitCredential] = {
    getPasswordFromFileOrUi(key) match {
      case Left(value) => Left(value)
      case Right(value) =>
        save(key, value)
        Right(value)
    }
  }

  private def getPasswordFromFileOrUi(key: String): Either[UserCanceled, GitCredential] = {
    readPasswordFileFor(key) match {
      case Left(_) =>
        deleteStoredCredentials(key)
        GitCredentialUi.getView(key).showAndWait().get()
      case Right(value) => Right(value)
    }
  }

  private def readPasswordFileFor(key: String): Either[LockbookError, GitCredential] = {
    fileHelper
      .readFile(s"$credentialFolder/$key")
      .map(EncryptedValue)
      .flatMap(encryptionHelper.decrypt(_, passwordHelper.password))
      .map(_.secret)
      .flatMap(GitCredential.deserialize)
  }

  private def save(key: String, gitCredential: GitCredential): Either[LockbookError, Unit] = {
    val decryptedValue = DecryptedValue(secret = GitCredential.serialize(gitCredential))

    encryptionHelper
      .encrypt(decryptedValue, passwordHelper.password)
      .map(_.garbage)
      .flatMap(fileHelper.saveToFile(new File(s"$credentialFolder/key"), _))
  }

  override def deleteStoredCredentials(key: String): Either[FileError, Unit] = {
    val file = new File(s"$credentialFolder/$key")
    try {
      if (file.delete()) {
        Right(())
      } else {
        Left(UnableToDelateFile(file))
      }
    } catch {
      case sec: SecurityException => Left(UnableToAccessFile(file, sec))
    }
  }
}
