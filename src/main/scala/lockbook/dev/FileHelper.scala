package lockbook.dev

import java.io.IOException
import java.nio.file.{Files, InvalidPathException, Paths}

import scala.util.{Failure, Success, Try}

trait FileHelper {
  def readFile(path: String): Try[String]
}
class FileHelperImpl extends FileHelper {
  override def readFile(path: String): Try[String] = {
    try {
      Success(Files.readString(Paths.get(path)))
    } catch {
      case _: InvalidPathException =>
        Failure(Errors.invalidPath)
      case _: IOException =>
        Failure(Errors.fileMissing)
      case _: OutOfMemoryError =>
        Failure(new Error("File is too large to be loaded into memory"))
      case _: SecurityException =>
        Failure(new Error("Unable to read to read from file due to file permissions"))
      case _ =>
        Failure(new Error("An unknown error occurred while trying to read this file into memory"))
    }
  }
}
