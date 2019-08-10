package lockbook.dev

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait FileHelper {
  def readFile(path: String): Future[String]
  def recursiveFileDelete(file: File): Future[Unit]
  def saveToFile(file: File, content: String): Future[Unit]
}
class FileHelperImpl extends FileHelper {
  override def readFile(path: String): Future[String] =
    Future { Files.readString(Paths.get(path)) }

  def recursiveFileDelete(directoryToBeDeleted: File): Future[Unit] = Future {
    val allContents = directoryToBeDeleted.listFiles
    if (allContents != null) for (file <- allContents) {
      recursiveFileDelete(file)
    }
    directoryToBeDeleted.delete
  }

  override def saveToFile(file: File, content: String): Future[Unit] = Future {
    Future {
      file.getParentFile.mkdirs
      file.createNewFile

      val pw = new PrintWriter(file)
      pw.write(content)
      pw.close()
      pw
    } andThen {
      case Success(pw) =>
        if (pw.checkError())
          Future.failed(new Error("Something went wrong while writing to this file"))
        else
          Future.successful(())
      case Failure(_) =>
        Future.failed(new Error(s"I do not have write access to ${file.getAbsoluteFile}"))
    }
  }
}
