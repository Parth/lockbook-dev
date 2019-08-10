package lockbook.dev

import java.io.{File, PrintWriter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait LockfileHelper {
  def getLockfile: Future[EncryptedValue]
  def writeToLockfile(string: String): Try[Unit]
}

class LockfileHelperImpl(encryptionHelper: EncryptionHelper, fileHelper: FileHelper)
    extends LockfileHelper {

  val lockfile = s"${App.path}/lockfile"

  def getLockfile: Future[EncryptedValue] = fileHelper.readFile(lockfile).map(EncryptedValue)

  def writeToLockfile(string: String): Try[Unit] = {
    try {
      val file = new File(lockfile)
      file.getParentFile.mkdirs
      file.createNewFile

      val pw = new PrintWriter(new File(lockfile)) // TODO file?
      pw.write(string)
      pw.close()

      if (pw.checkError()) {
        Failure(new Error("Something went wrong while writing to the lockfile"))
      } else {
        Success(())
      }
    } catch {
      case _: SecurityException =>
        Failure(new Error(s"I do not have write access to $lockfile"))
    }
  }
}
