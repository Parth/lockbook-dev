package devbook

import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, GridPane, HBox}
import javafx.stage.Stage
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
    newRepoButton.setOnAction(_ => {
      // Dialog
      val dialog = new Stage
      val cloneButton: ButtonType = new ButtonType("Clone")
      dialog.setTitle("Add repository to Lockbook")

      val grid = new GridPane
      grid.setHgap(10)
      grid.setVgap(10)

      val repositoryURL = new TextField

      grid.add(new Label("Repository Url:"), 0, 0);
      grid.add(repositoryURL, 1, 0)

      val comboBox = new ComboBox[String]
      comboBox.getItems.addAll(
        "Encrypted",
        "Not Encrypted"
      )

      comboBox.setValue("Encrypted")
      grid.add(comboBox, 1, 1)

      val cancel = new Button("Cancel")
      cancel.setOnAction(_ => {
        dialog.close()
      })
      grid.add(cancel, 1, 2)

      val clone = new Button("Clone")
      clone.setOnAction(_ => {
        primaryStream.onNext()
      })
      grid.add(clone, 2, 2)

      grid.setPadding(new Insets(20, 150, 10, 10))

      dialog.setScene(new Scene(grid, 500, 300))
      dialog.show()
    })
  }
}
