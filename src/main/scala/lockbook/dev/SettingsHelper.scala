package lockbook.dev

import java.io.File
import java.util.concurrent.TimeUnit

import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.concurrent.duration.FiniteDuration

trait SettingsHelper {
  def getTheme: Theme
  def getAutoLock: AutoLock
}

object SettingsHelper {
  val jsonPath = s"${App.path}/lockbook.json"

  def fromFile(fileHelper: FileHelper): LockbookSettings =
    fileHelper
      .readFile(jsonPath)
      .flatMap(decodeSettings) match {
      case Left(error) =>
        println(s"Could read settings: $error")
        LockbookSettings(None, None)
      case Right(settings) => settings
    }

  private def decodeSettings(s: String): Either[DecodingError, LockbookSettings] = { // TODO: Becoming convoluted, sort and optimize

    implicit val decodeAutoLock: Decoder[AutoLock] = Decoder.decodeLong.emap {
      case value: Long => Right(AutoLock(Some(FiniteDuration(value, TimeUnit.MINUTES))))
    }

    implicit val decodeTheme: Decoder[Theme] = Decoder.decodeString.emap {
      case Light.themeName => Right(Light)
      case unknown: String => Left(unknown)
    }

    implicit val decodeLockbookSettings: Decoder[LockbookSettings] = new Decoder[LockbookSettings] {
      final def apply(c: HCursor): Decoder.Result[LockbookSettings] =
        for {
          theme    <- c.downField("theme").as[Option[Theme]]
          autoLock <- c.downField("autoLock").as[Option[AutoLock]]
        } yield {
          new LockbookSettings(theme, autoLock)
        }
    }

    decode[LockbookSettings](s) match {
      case Left(error)     => Left(DecodingError(s, error))
      case Right(settings) => Right(settings)
    }
  }

  def constructJson(settings: LockbookSettings, fileHelper: FileHelper): Unit = { // TODO: Becoming convoluted, sort and optimize

    implicit val encodeLockbookSettings: Encoder[LockbookSettings] = new Encoder[LockbookSettings] {
      final def apply(a: LockbookSettings): Json = Json.obj(
        ("theme", Json.fromString(a.theme.getOrElse(Light).themeName)),
        ("autoLock", Json.fromLong(a.autoLockTime.getOrElse(AutoLock(Some(FiniteDuration(5, TimeUnit.MINUTES)))).time.get.toMinutes))
      )
    }

    val jsonFile = new File(jsonPath)
    if (!jsonFile.exists()) {
      jsonFile.createNewFile()
    }
    fileHelper.saveToFile(jsonFile, settings.asJson.noSpaces)
  }
}

class SettingsHelperImpl(settings: LockbookSettings) extends SettingsHelper { //und better
  override def getTheme: Theme = settings.theme.getOrElse(Light)
  override def getAutoLock: AutoLock =
    settings.autoLockTime.getOrElse(AutoLock(Some(FiniteDuration(5, TimeUnit.MINUTES))))
}
