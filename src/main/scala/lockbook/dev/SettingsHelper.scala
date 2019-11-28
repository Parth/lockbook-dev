package lockbook.dev

import io.circe.generic.auto._
import io.circe.parser._

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
        println(error)
        Settings(None, None)
      case Right(settings) => settings
    }

  private def decodeSettings(s: String): Either[DecodingError, Settings] =
    decode[Settings](s) match {
      case Left(error)     => Left(DecodingError(s, error))
      case Right(settings) => Right(settings)
    }

}

class SettingsHelperImpl(setting: Settings) extends SettingsHelper {
  override def getTheme: String     = setting.theme.getOrElse("default.css")
  override def getAutoLockTime: Int = setting.autoLockTime.getOrElse(5)
}
