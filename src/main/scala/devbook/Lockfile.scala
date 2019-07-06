package devbook
import java.io.{File, IOException, _}
import java.nio.file.{Files, Paths}

object Lockfile {

  def getLockfile: Option[EncryptedValue] = {
    try {
      Some(
        EncryptedValue(
          new String(
            Files.readAllBytes(
              Paths.get(App.lockfile)
            )
          )
        )
      )
    } catch {
      case _: IOException => None
    }
  }

  def createLockfile(passwordSuccess: PasswordSuccess): Option[Error] = {
    try {
      val file = new File(App.lockfile)
      file.getParentFile.mkdirs
      file.createNewFile
      val pw = new PrintWriter(new File(App.lockfile))
      val a = Encryption.encrypt(DecryptedValue("unlocked"), passwordSuccess)
      a match {
        case Left(encryptedValue) =>
          pw.write(encryptedValue.garbage) // TODO pw.write suppresses IOExceptions and returns nothing... wtf?
          pw.close()
          None
        case Right(error) =>
          Some(error)
      }
    } catch {
      case _: SecurityException =>
        Some(new Error(s"I do not have write access to ${App.lockfile}"))
    }

  }
}
