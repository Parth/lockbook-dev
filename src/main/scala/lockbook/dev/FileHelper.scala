package lockbook.dev

import java.io.{File, IOException}
import java.nio.file.{Files, InvalidPathException, Paths}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait FileHelper {
  def readFile(path: String): Future[String]
  def recursiveFileDelete(file: File): Future[Unit]
}
class FileHelperImpl extends FileHelper {
  override def readFile(path: String): Future[String] = {
    Future { Files.readString(Paths.get(path)) } recoverWith {
      case _: InvalidPathException =>
        Future.failed(Errors.invalidPath)
      case _: IOException =>
        Future.failed(Errors.fileMissing)
      case _: OutOfMemoryError =>
        Future.failed(new Error("File is too large to be loaded into memory"))
      case _: SecurityException =>
        Future.failed(new Error("Unable to read to read from file due to file permissions"))
      case _ =>
        Future.failed(
          new Error("An unknown error occurred while trying to read this file into memory")
        )
    }
  }

  def recursiveFileDelete(directoryToBeDeleted: File): Future[Unit] = Future {
    val allContents = directoryToBeDeleted.listFiles
    if (allContents != null) for (file <- allContents) {
      recursiveFileDelete(file)
    }
    directoryToBeDeleted.delete
  }
}
