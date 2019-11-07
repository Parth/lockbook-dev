package lockbook.dev

import java.io.{File, PrintWriter}

import net.liftweb.json
import net.liftweb.json.DefaultFormats

import scala.io.Source

case class Settings( // make members of settings optional
    theme: String,
    autoLockTime: Int
)

trait SettingsHelper {
  def constructJson: Unit
  def getSettings: Settings
}

object SettingsHelper {
  def fromFile: Settings = { // read file, turn it into a case class

  }
}

class SettingsHelperImpl(settings: Settings) { // Maybe make Settings into an object

  val jsonFile = s"${App.path}/lockbook.json"

  def constructJson: Unit = { //
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
      val stream = Source.fromFile(new File(jsonFile))
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
