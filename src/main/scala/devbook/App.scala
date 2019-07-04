package devbook

import javafx.application.Application
import javafx.stage.Stage

object App {
  def main(args: Array[String]) {
    Application.launch(classOf[App], args: _*)
  }

class App extends Application {
  override def start(primaryStage: Stage): Unit = {

  }
    primaryStage.setTitle("Hello World!")
  }
}
