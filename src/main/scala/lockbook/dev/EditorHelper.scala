package lockbook.dev

import java.io.{BufferedWriter, File, FileWriter}

import org.eclipse.jgit.api.Git

import scala.util.Try

trait EditorHelper {
  def getTextFromFile(f: File): Try[String]
  def saveCommitAndPush(message: String, file: String, originalFile: File, git: Git): Try[Unit]
}

class EditorHelperImpl(
    encryptionHelper: EncryptionHelper,
    passwordHelper: PasswordHelper,
    gitHelper: GitHelper,
    fileHelper: FileHelper
) extends EditorHelper {

  override def getTextFromFile(f: File): Try[String] = {
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
  ): Try[Unit] = {

    val contentToSave: String = if (originalFile.getName.endsWith("aes")) {
      encryptionHelper
        .encrypt(DecryptedValue(content), passwordHelper.password)
        .get
        .garbage // TODO
    } else {
      content
    }

    saveFile(contentToSave, originalFile)

    gitHelper.commitAndPush(message, git)
  }

  private def saveFile(contents: String, file: File): Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(contents)
    bw.close() // TODO
  }
}
