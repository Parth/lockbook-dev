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

    implicit val decodeAutoLock: Decoder[AutoLock] = Decoder.decodeString.emap {
      case "None" => Right(AutoLock(None))
      case value  => Right(AutoLock(Some(FiniteDuration(value.asInstanceOf[Long], TimeUnit.MINUTES))))
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

    implicit val encodeFiniteDuration: Encoder[FiniteDuration] = new Encoder[FiniteDuration] {
      override def apply(a: FiniteDuration): Json = a.toMinutes.asInstanceOf[Json]
    }

    implicit val encodeAutoLock: Encoder[AutoLock] = new Encoder[AutoLock] {
      override def apply(a: AutoLock): Json = {
        if (a.time.isEmpty) Json.fromString(None.toString)
        else Json.fromLong(a.time.get.toMinutes)
      }
    }

    implicit val encodeLockbookSettings: Encoder[LockbookSettings] =
      new Encoder[LockbookSettings] { // does not correctly add None to json
        final def apply(a: LockbookSettings): Json = Json.obj(
          ("theme", Json.fromString(a.theme.getOrElse(Light).themeName)),
          ("autoLock", a.autoLockTime.asJson)
        )
      }

    val jsonFile = new File(jsonPath)
    if (!jsonFile.exists()) jsonFile.createNewFile()
    fileHelper.saveToFile(jsonFile, settings.asJson.noSpaces)
  }
}

class SettingsHelperImpl(settings: LockbookSettings) extends SettingsHelper { //und better
  override def getTheme: Theme = settings.theme.getOrElse(Light)
  override def getAutoLock: AutoLock =
    settings.autoLockTime.getOrElse(AutoLock(Some(FiniteDuration(5, TimeUnit.MINUTES))))
}
