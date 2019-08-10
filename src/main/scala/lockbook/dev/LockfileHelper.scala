package lockbook.dev

import java.io.File

trait LockfileHelper {
  def getLockfile: Either[FileError, EncryptedValue]
  def writeToLockfile(value: EncryptedValue): Either[FileError, Unit]
}

class LockfileHelperImpl(encryptionHelper: EncryptionHelper, fileHelper: FileHelper)
    extends LockfileHelper {

  val lockfile = s"${App.path}/lockfile"

  def getLockfile: Either[FileError, EncryptedValue] =
    fileHelper.readFile(lockfile).map(EncryptedValue)

  def writeToLockfile(value: EncryptedValue): Either[FileError, Unit] =
    fileHelper.saveToFile(new File(lockfile), value.garbage)

}
