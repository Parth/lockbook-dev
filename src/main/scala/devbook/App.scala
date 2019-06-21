package devbook

import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.StackPane
import javafx.stage.Stage

object App {
  def main(args: Array[String]) {
    Application.launch(classOf[App], args: _*)
  }
}

class App extends Application {
  override def start(primaryStage: Stage) {
    primaryStage.setTitle("Hello World!")
    val btn = new Button
    btn.setText("Say 'Hello World'")
    btn.setOnAction((e: ActionEvent) => {
      println("Hello World!")
    })

    val root = new StackPane
    root.getChildren.add(btn)
    primaryStage.setScene(new Scene(root, 300, 250))
    primaryStage.show
  }

}
