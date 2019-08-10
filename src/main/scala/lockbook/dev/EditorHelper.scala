package lockbook.dev

import java.io.File

import org.eclipse.jgit.api.Git

trait EditorHelper {
  def getTextFromFile(f: File): Either[LockbookError, String]
  def saveCommitAndPush(
      message: String,
      file: String,
      originalFile: File,
      git: Git
  ): Either[LockbookError, Unit]
}

class EditorHelperImpl(
    encryptionHelper: EncryptionHelper,
    passwordHelper: PasswordHelper,
    gitHelper: GitHelper,
    fileHelper: FileHelper
) extends EditorHelper {

  override def getTextFromFile(f: File): Either[LockbookError, String] = {
    if (f.getName.endsWith("aes")) {
      fileHelper
        .readFile(f.getAbsolutePath)
        .map(EncryptedValue)
        .flatMap(encryptionHelper.decrypt(_, passwordHelper.password))
        .map(_.secret)
    } else {
      fileHelper.readFile(f.getAbsolutePath)
    }
  }

  override def saveCommitAndPush(
      message: String,
      content: String,
      originalFile: File,
      git: Git
  ): Either[LockbookError, Unit] = {

    val contentToSave: Either[CryptoError, String] = if (originalFile.getName.endsWith("aes")) {
      encryptionHelper
        .encrypt(DecryptedValue(content), passwordHelper.password)
        .map(_.garbage)
    } else {
      Right(content)
    }

    contentToSave
      .flatMap(fileHelper.saveToFile(originalFile, _))
      .flatMap(_ => gitHelper.commitAndPush(message, git))
  }
}
