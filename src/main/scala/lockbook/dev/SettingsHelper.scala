package lockbook.dev

import java.io.File
import java.util.concurrent.TimeUnit

import io.circe.{ACursor, Decoder, FailedCursor, HCursor}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration.FiniteDuration

trait SettingsHelper {
  def getTheme: Theme
  def getAutoLockTime: AutoLockTime
}

object SettingsHelper {
  val jsonPath = s"${App.path}/lockbook.json"

  def fromFile(fileHelper: FileHelper): LockbookSettings =
    fileHelper
      .readFile(jsonPath)
      .flatMap(decodeSettings) match {
      case Left(error) =>
        println(s"Could read settings: $error")
        LockbookSettings(Light, AutoLockTime())
      case Right(settings) => settings
    }

  private def matchTheme(s: String): Either[UnableToFindSetting, Theme] =
    s match {
      case Light.fileName => Right(Light)
      case _ => Left(UnableToFindSetting(s))
    }

  private def matchAutoLockTime(time: Long): AutoLockTime = {
    time match {
      case 0 => AutoLockTime(None)
      case _ => AutoLockTime(Some(FiniteDuration(time, TimeUnit.MINUTES)))
    }
  }

  private def decodeSettings(s: String): Either[DecodingError, LockbookSettings] = {

    implicit val decodeTheme: Theme = (d: Decoder[AutoLockTime]) => {
      d.get[String]("theme").flatMap(matchTheme) match {
        case Left(error) =>
          println(s"Could not read theme: $error")
          Light
        case Right(theme) => theme
      }
    }

    implicit val decodeAutoLockTime: AutoLockTime = (d: Decoder[Theme]) => {
      d.get[Long]("autoLockTime") match {
        case Left(error) =>
          println(s"Could not read autoLockTime $error")
          AutoLockTime(Some(FiniteDuration(5, TimeUnit.MINUTES)))
        case Right(time) => matchAutoLockTime(time)
      }
    }

    implicit val decoder: Decoder[LockbookSettings] = (c: HCursor) => {
      for {
        theme <- c.get[Theme]("theme")
        autoLockTime <- c.get[AutoLockTime]("autoLockTime")
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
  override def getTheme: Theme = settings.theme.getOrElse(Light)
  override def getAutoLockTime: AutoLockTime =
    settings.autoLockTime.getOrElse(AutoLockTime(Some(FiniteDuration(5, TimeUnit.MINUTES))))
}
