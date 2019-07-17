package devbook

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths}

import org.eclipse.jgit.api.Git

trait EditorHelper {
  def getTextFromFile(f: File): Either[String, Error]
  def saveCommitAndPush(message: String, file: String, originalFile: File, git: Git): Option[Error]
}

class EditorHelperImpl(
    encryptionHelper: EncryptionHelper,
    passwordHelper: PasswordHelper,
    gitHelper: GitHelper
) extends EditorHelper {
  override def getTextFromFile(f: File): Either[String, Error] = {
    if (f.getName.endsWith("aes")) {
      val base64Junk = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath)))
      val encrypted  = EncryptedValue(base64Junk)

      encryptionHelper.decrypt(encrypted, passwordHelper.password.get) match {
        case Left(decryptedValue) => Left(decryptedValue.secret)
        case Right(error)         => Right(error)
      } // TODO swap all the either so we can use maps

    } else {
      Left(new String(Files.readAllBytes(Paths.get(f.getAbsolutePath))))
    }
  }

  override def saveCommitAndPush(
      message: String,
      content: String,
      originalFile: File,
      git: Git
  ): Option[Error] = {
    None

    val contentToSave: String = if (originalFile.getName.endsWith("aes")) {
      encryptionHelper
        .encrypt(DecryptedValue(content), passwordHelper.password.get)
        .left
        .get
        .garbage // TODO
    } else {
      content
    }

    saveFile(contentToSave, originalFile)

    gitHelper.commitAndPush(message, git)

    None
  }

  private def saveFile(contents: String, file: File): Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(contents)
    bw.close() // TODO
  }
}
