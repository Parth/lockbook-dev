package lockbook.dev

import java.io.{File, FileNotFoundException, IOException, PrintWriter}
import java.nio.file.{Files, Paths}

trait FileHelper {
  def readFile(path: String): Either[FileError, String]
  def recursiveFileDelete(file: File): Either[FileError, Unit]
  def saveToFile(file: File, content: String): Either[FileError, Unit]
}

class FileHelperImpl extends FileHelper {
  override def readFile(path: String): Either[FileError, String] = {
    try {
      if (new File(path).isDirectory)
        Left(FileIsFolder(new File(path)))
      else
        Right(Files.readString(Paths.get(path)))
    } catch {
      case ioe: IOException       => Left(UnableToReadFile(new File(path), ioe))
      case oom: OutOfMemoryError  => Left(FileTooBig(new File(path), oom))
      case sec: SecurityException => Left(UnableToAccessFile(new File(path), sec))
    }
  }

  def recursiveFileDelete(directoryOrFileToBeDeleted: File): Either[FileError, Unit] = {
    try {
      val allContents = directoryOrFileToBeDeleted.listFiles
      if (allContents != null)
        for (file <- allContents) {
          recursiveFileDelete(file)
        }
      directoryOrFileToBeDeleted.delete
      Right(())
    } catch {
      case sec: SecurityException => Left(UnableToAccessFile(directoryOrFileToBeDeleted, sec))
    }
  } // TODO this leaks errors

  override def saveToFile(file: File, content: String): Either[FileError, Unit] = {
    try {
      file.getParentFile.mkdirs
      file.createNewFile

      val pw = new PrintWriter(file)
      pw.write(content)
      pw.close()
      if (pw.checkError()) {
        Left(UnableToWrite(file))
      } else {
        Right(())
      }
    } catch {
      case fnf: FileNotFoundException => Left(UnableToReadFile(file, fnf))
      case sec: SecurityException     => Left(UnableToAccessFile(file, sec))
    }
  }
}
