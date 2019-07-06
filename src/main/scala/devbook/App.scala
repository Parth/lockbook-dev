package devbook

import javafx.application.Application
import javafx.stage.Stage

object App {

  val path = s"${System.getProperty("user.home")}/.devbook"
  val lockfile = s"$path/lockfile"

  def main(args: Array[String]) {
    Application.launch(classOf[App], args: _*)
  }
}

class App extends Application {
  override def start(primaryStage: Stage): Unit = {
    UI(primaryStage).setup()
  }
}
