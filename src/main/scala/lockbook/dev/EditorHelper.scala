package lockbook.dev

import java.io.File

trait EditorHelper {
  def getTextFromFile(f: File): Either[LockbookError, String]
  def save(file: String, originalFile: File): Either[LockbookError, Unit]
}

class EditorHelperImpl(
    encryptionHelper: EncryptionHelper,
    passphraseHelper: PassphraseHelper,
    fileHelper: FileHelper
) extends EditorHelper {

  override def getTextFromFile(f: File): Either[LockbookError, String] = {
    if (f.getName.endsWith("aes")) {
      fileHelper
        .readFile(f.getAbsolutePath)
        .map(EncryptedValue)
        .flatMap(encryptionHelper.decrypt(_, passphraseHelper.passphrase))
        .map(_.secret)
    } else {
      fileHelper.readFile(f.getAbsolutePath)
    }
  }

  override def save(content: String, originalFile: File): Either[LockbookError, Unit] = {

    val contentToSave: Either[CryptoError, String] = if (originalFile.getName.endsWith("aes")) {
      encryptionHelper
        .encrypt(DecryptedValue(content), passphraseHelper.passphrase)
        .map(_.garbage)
    } else {
      Right(content)
    }

    contentToSave
      .flatMap(fileHelper.saveToFile(originalFile, _))
  }
}
