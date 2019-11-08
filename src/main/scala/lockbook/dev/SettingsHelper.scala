package lockbook.dev

import java.io.{File, PrintWriter}

import net.liftweb.json
import net.liftweb.json.DefaultFormats // don't use this thing search for typesafe scala library for this 

import scala.io.Source

case class Settings( // make members of settings optional
    theme: Option[String],
    autoLockTime: Option[Int]
)

trait SettingsHelper { // Trait level should not return any options to anyone 
  def getTheme: String
}

class SettingHelperImpl(setting: Setting) extends SettingHelper { // This file is where all the default values will live
    override def getTheme: String = setting.theme.getOrElse("default.css")
    override def getTheme: String = setting.theme.getOrElse("default.css")
    override def getTheme: String = setting.theme.getOrElse("default.css")
    override def getTheme: String = setting.theme.getOrElse("default.css")
    override def getTheme: String = setting.theme.getOrElse("default.css")
}

object SettingsHelper {
  def fromFile(fileHelper: FileHelper): Settings = { // read file, turn it into a case class

  }
}

class SettingsHelperImpl(settings: Settings) { // Maybe make Settings into an object

  val jsonFile = s"${App.path}/lockbook.json"

  def constructJson: Unit = { // default values 
    val jsonString = """
      {
        "theme": "light.css"
        "autoLockTime": 5
      }
    """

    val pw = new PrintWriter(new File(jsonFile))
    pw.write(jsonString)
    pw.close
  }

  def getSettings: Settings = { // everytime you should access the settings file
    try {
      val stream = Source.fromFile(new File(jsonFile)) // use filehelper
      implicit val formats = DefaultFormats
      json.parse(stream.mkString).extract[Settings]
    } catch {
      case e: Exception => { // Catch exception in which file doesn't exist, I dont know what type of error it could release though
        constructJson
        getSettings
      }
    }
  }
}
