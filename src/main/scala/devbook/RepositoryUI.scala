package devbook

import javafx.scene.control.Button
import javafx.scene.layout.{BorderPane, HBox}
import rx.lang.scala.Subject

class RepositoryUI(primaryStream: Subject[Events]) {

  private val borderPane = new BorderPane
  private val newRepoButton = new Button("New Repository")
  private val footer = new HBox(newRepoButton)

  def getView: BorderPane = {
    borderPane.bottomProperty().set(footer)
    borderPane
  }

  def setupListeners(): Unit = {
    newRepoButton.setOnAction(event => {
      println(event)
    })
  }
}
