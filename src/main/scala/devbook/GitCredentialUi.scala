package devbook

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.layout.GridPane

import scala.util.{Failure, Success, Try}

object GitCredentialUi {
  def getView(credentialName: String): Dialog[Try[GitCredential]] = {

    val dialog = new Dialog[Try[GitCredential]]
    dialog.setTitle("Login Dialog")
    dialog.setHeaderText(s"Enter credentials for $credentialName")

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
        Success(GitCredential(username.getText, password.getText))
      case _ =>
        Failure(Errors.noCredentialsProvided)
    }

    dialog
  }
}
