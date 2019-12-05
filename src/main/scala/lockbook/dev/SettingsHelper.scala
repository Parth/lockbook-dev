package lockbook.dev

import java.io.File

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

case class Settings(
    theme: Option[String],
    autoLockTime: Option[Int]
)

trait SettingsHelper {
  def getTheme: String
  def getAutoLockTime: Int
}

object SettingsHelper {
  val jsonPath = s"${App.path}/lockbook.json"

  def fromFile(fileHelper: FileHelper): Settings =
    fileHelper
      .readFile(jsonPath)
      .flatMap(decodeSettings) match {
      case Left(error) =>
        println(s"Could read settings: $error")
        Settings(None, None)
      case Right(settings) => settings
    }

  private def decodeSettings(s: String): Either[DecodingError, Settings] =
    decode[Settings](s) match {
      case Left(error)     => Left(DecodingError(s, error))
      case Right(settings) => Right(settings)
    }

  def constructJson(settings: Settings, fileHelper: FileHelper): Unit = {
    val jsonFile = new File(jsonPath)
    if(!jsonFile.exists()) jsonFile.createNewFile()
    val jsonString      = settings.asJson.noSpaces
    fileHelper.saveToFile(jsonFile, jsonString)
  }
}

class SettingsHelperImpl(setting: Settings) extends SettingsHelper {
  override def getTheme: String     = setting.theme.getOrElse("default.css")
  override def getAutoLockTime: Int = setting.autoLockTime.getOrElse(5)
}
