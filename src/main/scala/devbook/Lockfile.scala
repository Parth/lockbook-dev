package devbook

import java.io.{File, IOException, PrintWriter}
import java.nio.file.{Files, Paths}

trait Lockfile {
  def getLockfile: Option[EncryptedValue]
  def createLockfile(passwordSuccess: Password): Option[Error]
}

class LockfileImpl(encryptionHelper: EncryptionHelper) extends Lockfile {

  val lockfile = s"${App.path}/lockfile"

  def getLockfile: Option[EncryptedValue] = {
    try {
      Some(
        EncryptedValue(
          new String(
            Files.readAllBytes(
              Paths.get(lockfile)
            )
          )
        )
      )
    } catch {
      case _: IOException => None
    }
  }

  def createLockfile(passwordSuccess: Password): Option[Error] = {
    try {
      val file = new File(lockfile)
      file.getParentFile.mkdirs
      file.createNewFile

      val pw = new PrintWriter(new File(lockfile))
      val a  = encryptionHelper.encrypt(DecryptedValue("unlocked"), passwordSuccess)
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
        Some(new Error(s"I do not have write access to $lockfile"))
    }
  }
}
