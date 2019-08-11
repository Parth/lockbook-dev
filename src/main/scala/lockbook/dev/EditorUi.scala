package lockbook.dev

import java.io.File

import javafx.application.Platform
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, HBox}
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditorUi(editorHelper: EditorHelper) {

  def getView(git: Git, f: File): BorderPane = {
    val root     = new BorderPane
    val textArea = new TextArea

    loadFile(git, f, root, textArea)

    root
  }

  private def loadFile(git: Git, f: File, root: BorderPane, textArea: TextArea): Future[Unit] =
    Future {
      editorHelper.getTextFromFile(f) match {
        case Right(string) =>
          Platform.runLater(() => {
            textArea.setText(string)
            textArea.setWrapText(true)
            root.setBottom(getBottom(git, f, textArea))
            root.setCenter(textArea)
          })
        case Left(error) =>
          Platform.runLater(
            () => root.setCenter(new Label(error.uiMessage))
          )
      }
    }

  def getBottom(git: Git, file: File, textArea: TextArea): HBox = {
    val save          = new Button("Push")
    val commitMessage = new TextField
    commitMessage.setPromptText("Commit Message")

    save.setOnAction(_ => {
      Future {
        editorHelper
          .saveCommitAndPush(commitMessage.getText, textArea.getText, file, git) match {
          case Right(_) =>
            Platform.runLater(() => {
              AlertUi.showGood("Push Successful", "Changes saved successfully.")
            })
          case Left(exception) =>
            Platform.runLater(() => {
              AlertUi.showBad("Push Failed", exception.uiMessage)
            })
        }
      }
    })

    new HBox(commitMessage, save)
  }
}
