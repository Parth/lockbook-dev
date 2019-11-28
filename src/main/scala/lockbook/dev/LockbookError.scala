package lockbook.dev

import java.io.{File, IOException}
import java.security.NoSuchAlgorithmException

import org.eclipse.jgit.api.errors.TransportException

sealed trait LockbookError { val uiMessage: String }

case class DecodingError(raw: String, e: io.circe.Error)  extends LockbookError {
  override val uiMessage: String =  s"$raw, could not be decoded, $e"
}

// Errors from FileHelper
trait FileError extends LockbookError
case class FileTooBig(f: File, oom: OutOfMemoryError) extends FileError {
  override val uiMessage = s"Too large to read into memory: ${f.getName}."
}
case class UnableToReadFile(f: File, ioe: IOException) extends FileError {
  override val uiMessage = s"Unable to read: ${f.getName}."
}

case class UnableToAccessFile(f: File, s: SecurityException) extends FileError {
  override val uiMessage = s"This process does not have permission to view ${f.getName}."
}

case class UnableToWrite(f: File) extends FileError {
  override val uiMessage = s"Unable to write to ${f.getName}."
}

case class UnableToDeleteFile(f: File) extends FileError {
  override val uiMessage = s"Unable to delete ${f.getName}."
}

case class FileIsFolder(f: File) extends FileError {
  override val uiMessage = s"${f.getName} is a folder."
}

// Errors from EncryptionHelper
trait CryptoError extends LockbookError
case class SecureOperationsNotSupported(e: NoSuchAlgorithmException) extends CryptoError {
  override val uiMessage = s"The current runtime does not support this encryption operation: ${e.getMessage}."
}

case class WrongPassphrase() extends CryptoError {
  override val uiMessage = "Decryption failed, incorrect passphrase."
}

case class NotBase64() extends CryptoError {
  override val uiMessage = "Content unreadable, not base64."
}

// Errors from GitHelper & GitCredentialHelper
trait GitError extends LockbookError

case class CouldNotStoreCredentials() extends GitError {
  override val uiMessage = "Failed to read stored credentials."
}

case class TransportExceptionError(te: TransportException) extends GitError {
  override val uiMessage: String = {
    if (te.getMessage.contains("cannot open")) "Network Error."
    else if (te.getMessage.contains("not authorized")) "Credentials Rejected"
    else "Network error or credentials rejected"
  }
}

case class UserCanceled() extends GitError {
  override val uiMessage = "No git credentials entered."
}

case class UnknownException(e: Exception) extends GitError {
  override val uiMessage: String = s"Unknown exception: ${e.getMessage}."
}

// User Input related errors
trait UserInputErrors extends LockbookError

case class PassphrasesDoNotMatch() extends UserInputErrors {
  override val uiMessage = "Passphrases do not match."
}
