package devbook

import java.io.File

import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, HBox}
import org.eclipse.jgit.api.Git

import scala.util.{Failure, Success}

class EditorUi(editorHelper: EditorHelper) {

  def getView(git: Git, f: File): BorderPane = {
    val root     = new BorderPane
    val textArea = new TextArea
    val text     = editorHelper.getTextFromFile(f)

    if (text.isSuccess) {
      textArea.setText(text.get) // TODO handle error
      textArea.setWrapText(true) // TODO make this an option?
      root.setBottom(getBottom(git, f, textArea))
      root.setCenter(textArea)
    } else {
      root.setCenter(new Label("This file is encrypted with a different password"))
    }
    root
  }

  def getBottom(git: Git, file: File, textArea: TextArea): HBox = {
    val save          = new Button("Push")
    val commitMessage = new TextField
    commitMessage.setPromptText("Commit Message")

    save.setOnAction(_ => {
      editorHelper.saveCommitAndPush(commitMessage.getText, textArea.getText, file, git) match {
        case Success(_) =>
          val alert = new Alert(AlertType.CONFIRMATION)
          alert.setTitle("Push Successful")
          alert.show()

        case Failure(exception) =>
          val alert = new Alert(AlertType.CONFIRMATION)
          println(exception)
          alert.setTitle("Push Successful")
          alert.setHeaderText("Look, an Information Dialog")
          alert.setContentText("I have a great message for you!")
      }
    })

    new HBox(commitMessage, save)
  }
}
