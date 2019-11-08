package lockbook.dev

import java.io.File
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

case class Settings( // make members of settings optional
    theme: Option[String],
    autoLockTime: Option[Int]
)

trait SettingsHelper { // Trait level should not return any options to anyone
  def getTheme: String
  def getAutoLockTime: Int
}

object SettingsHelper {
  val jsonPath = s"${App.path}/lockbook.json"

  def fromFile(fileHelper: FileHelper): Settings = { // read file, turn it into a case class
    fileHelper.readFile(jsonPath) match {
      case Left(fileError) => {
        if(fileError.isInstanceOf[UnableToReadFile]) {
          constructJson(jsonPath, fileHelper)
          checkJson(jsonPath, fileHelper)
        } else {
          Settings(None, None)
        }
      }
      case Right(jsonString) => {
        val settingsClass = decode[Settings](jsonString)
        settingsClass match { // could make function
          case Left(error) => Settings(None, None)
          case Right(settings) => settings
        }
      }
    }
  }

  private def constructJson(jsonPath: String, fileHelper: FileHelper): Unit = {
    val jsonFile = new File(jsonPath)
    jsonFile.createNewFile()
    val defaultSettings = Settings(Some("default.css"), Some(5))
    val jsonString      = defaultSettings.asJson.noSpaces
    fileHelper.saveToFile(jsonFile, jsonString)
  }

  private def checkJson(jsonPath: String, fileHelper: FileHelper): Settings = {
    fileHelper.readFile(jsonPath) match {
      case Left(fileError) => {
        Settings(None, None)
      }
      case Right(jsonString) => {
        val settingsClass = decode[Settings](jsonString)
        settingsClass match {
          case Left(error) => Settings(None, None)
          case Right(settings) => settings
        }
      }
    }
  }
}

class SettingsHelperImpl(setting: Settings) extends SettingsHelper { // This file is where all the default values will live
  override def getTheme: String     = setting.theme.getOrElse("default.css")
  override def getAutoLockTime: Int = setting.autoLockTime.getOrElse(5)
}
