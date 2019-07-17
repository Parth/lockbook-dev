package devbook

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.layout.GridPane

case class GitCredentials(username: String, password: String)

trait GitCredentialHelper {
  def getCredentials: GitCredentials
}

class GitCredentialHelperImpl extends GitCredentialHelper {
  var credentials: Option[GitCredentials] = None

  def getView: Dialog[Option[GitCredentials]] = {

    val dialog = new Dialog[Option[GitCredentials]]
    dialog.setTitle("Login Dialog")
    dialog.setHeaderText("Look, a Custom Login Dialog")

    val loginButtonType = new ButtonType("Login", ButtonData.OK_DONE)
    dialog.getDialogPane.getButtonTypes.addAll(loginButtonType, ButtonType.CANCEL)

    val grid = new GridPane
    grid.setHgap(10)
    grid.setVgap(10)
    grid.setPadding(new Insets(20, 150, 10, 10))

    val username = new TextField
    username.setPromptText("Username")
    val password = new PasswordField
    password.setPromptText("Password")

    grid.add(new Label("Username:"), 0, 0)
    grid.add(username, 1, 0)
    grid.add(new Label("Password:"), 0, 1)
    grid.add(password, 1, 1)

    val loginButton = dialog.getDialogPane.lookupButton(loginButtonType)

    dialog.getDialogPane.setContent(grid)

    Platform.runLater(() => username.requestFocus())

    dialog.setResultConverter {
      case loginButtonType =>
        Some(GitCredentials(username.getText, password.getText))
      case _ =>
        None
    }

    dialog
  }

  override def getCredentials: GitCredentials = {
    credentials match {
      case Some(value) =>
        value
      case None =>
        getView.showAndWait().get().get // TODO
    }
  }
}
