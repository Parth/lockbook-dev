package lockbook.dev

import java.io.File
import java.util.concurrent.TimeUnit

import io.circe.{Decoder, HCursor}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration.FiniteDuration

trait SettingsHelper {
  def getTheme: String
  def getAutoLockTime: Int
}

object SettingsHelper {
  val jsonPath = s"${App.path}/lockbook.json"

  def fromFile(fileHelper: FileHelper): LockbookSettings =
    fileHelper
      .readFile(jsonPath)
      .flatMap(decodeSettings) match {
      case Left(error) =>
        println(s"Could read settings: $error")
        LockbookSettings(Light, None)
      case Right(settings) => settings
    }

  private def decodeSettings(s: String): Either[DecodingError, LockbookSettings] = {
    implicit val decodeTheme: Theme = {
      
    }

    implicit val decoder: Decoder[LockbookSettings] = (c: HCursor) => {
      for {
        theme <- c.downField("theme").as[Theme]
        autoLockTime <- c.downField("autoLockTime").as[Option[AutoLockTime]]
      } yield new LockbookSettings(theme, autoLockTime)
    }

    decode[LockbookSettings](s) match {
      case Left(error)     => Left(DecodingError(s, error))
      case Right(settings) => Right(settings)
    }
  }

  def constructJson(settings: LockbookSettings, fileHelper: FileHelper): Unit = {
    val jsonFile = new File(jsonPath)
    if (!jsonFile.exists()) jsonFile.createNewFile()
    val jsonString = settings.asJson.noSpaces
    fileHelper.saveToFile(jsonFile, jsonString)
  }

}

class SettingsHelperImpl(settings: LockbookSettings) extends SettingsHelper { //und better
  override def getTheme: String = settings.theme.fileName
  override def getAutoLockTime: Int = // should I just keep this as finiteduration
    settings.autoLockTime
      .getOrElse(AutoLockTime(Some(FiniteDuration(5, TimeUnit.MINUTES))))
      .time
      .getOrElse(FiniteDuration(5, TimeUnit.MINUTES))
      .toMinutes
      .asInstanceOf[Int]
}
