package lockbook.dev

import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.layout.GridPane
import javafx.stage.Stage
import org.eclipse.jgit.api.Git

import scala.util.{Failure, Success}

class CloneRepoDialog(gitHelper: GitHelper) {

  def showDialog(repoList: ObservableList[Git]): Unit = {
    val dialog = new Stage
    dialog.setTitle("Add repository to Lockbook")

    val grid = new GridPane
    grid.setHgap(10)
    grid.setVgap(10)

    val repositoryURL = new TextField

    grid.add(new Label("Repository Url:"), 0, 0)
    grid.add(repositoryURL, 1, 0)

    val cancel = new Button("Cancel")
    cancel.setOnAction(_ => {
      dialog.close()
    })
    grid.add(cancel, 1, 1)

    val clone = new Button("Clone")
    clone.setOnAction(_ => {

      gitHelper.cloneRepository(repositoryURL.getText) match {
        case Success(value) =>
          repoList.add(value)
          dialog.close()
        case Failure(error) =>
          AlertUi.showBad("Failed to clone repository:", error.getMessage)
      }
    })
    grid.add(clone, 2, 1)

    grid.setPadding(new Insets(20, 150, 10, 10))

    dialog.setScene(new Scene(grid, 500, 300))
    dialog.getScene.getStylesheets.add("dark.css")
    dialog.show()
  }
}
