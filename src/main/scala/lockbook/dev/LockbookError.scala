package lockbook.dev

import java.io.{File, IOException}
import java.security.NoSuchAlgorithmException

sealed trait LockbookError { val uiMessage: String }

// Errors from FileHelper
trait FileError extends LockbookError
case class FileTooBig(f: File, oom: OutOfMemoryError) extends FileError {
  val uiMessage = s"Too large to read into memory: ${f.getName}"
}
case class UnableToReadFile(f: File, ioe: IOException) extends FileError {
  val uiMessage = s"Unable to read: ${f.getName}"
}

case class UnableToAccessFile(f: File, s: SecurityException) extends FileError {
  val uiMessage = s"This process does not have permission to view ${f.getName}"
}

case class UnableToWrite(f: File) extends FileError {
  val uiMessage = s"Unable to write to ${f.getName}"
}

case class UnableToDelateFile(f: File) extends FileError {
  val uiMessage = s"Unable to delete ${f.getName}"
}

case class FileIsFolder(f: File) extends FileError {
  val uiMessage = s"${f.getName} is a folder"
}

// Errors from EncryptionHelper
trait CryptoError extends LockbookError
case class SecureOperationsNotSupported(e: NoSuchAlgorithmException) extends CryptoError {
  val uiMessage = s"The current runtime does not support this encryption operation: ${e.getMessage}"
}

case class WrongPassphrase() extends CryptoError {
  val uiMessage = "Decryption failed, incorrect passphrase"
}

case class NotBase64() extends CryptoError {
  val uiMessage = "Content unreadable, not base64"
}

// Errors from GitHelper & GitCredentialHelper
trait GitError extends LockbookError

case class CouldNotStoreCredentials() extends GitError {
  val uiMessage = "Failed to read stored credentials"
}

case class InvalidCredentials() extends GitError {
  val uiMessage = "Saved Credentials were invalid, please retry."
}

case class UserCanceled() extends GitError {
  val uiMessage = "No git credentials entered"
}

// User Input related errors
trait UserInputErrors extends LockbookError

case class PassphrasesDoNotMatch() extends UserInputErrors {
  val uiMessage = "Passphrases do not match."
}
