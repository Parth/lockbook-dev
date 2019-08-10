package lockbook.dev

import java.io.File

import javafx.application.Platform
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, HBox}
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class EditorUi(editorHelper: EditorHelper) {

  def getView(git: Git, f: File): BorderPane = {
    val root     = new BorderPane
    val textArea = new TextArea

    editorHelper.getTextFromFile(f) onComplete {
      case Success(string) =>
        Platform.runLater(() => {
          textArea.setText(string)
          textArea.setWrapText(true)
          root.setBottom(getBottom(git, f, textArea))
          root.setCenter(textArea)
        })
      case Failure(_) =>
        Platform.runLater(
          () => root.setCenter(new Label("This file is encrypted with a different password"))
        )
    }

    root
  }

  def getBottom(git: Git, file: File, textArea: TextArea): HBox = {
    val save          = new Button("Push")
    val commitMessage = new TextField
    commitMessage.setPromptText("Commit Message")

    save.setOnAction(_ => {
      editorHelper
        .saveCommitAndPush(commitMessage.getText, textArea.getText, file, git) onComplete {
        case Success(_) =>
          Platform.runLater(() => {
            AlertUi.showGood("Push Successful", "Changes saved successfully.")
          })
        case Failure(exception) =>
          Platform.runLater(() => {
            AlertUi.showBad("Push Failed", exception.getMessage)
          })
      }
    })

    new HBox(commitMessage, save)
  }
}
