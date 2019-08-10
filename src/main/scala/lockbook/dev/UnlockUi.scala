package lockbook.dev

import javafx.application.Platform
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UnlockUi(passwordHelper: PasswordHelper) {
  def getView(passwordSuccess: => Unit): VBox = {

    val vBox                            = new VBox
    val prompt                          = new Label("Enter your passphrase")
    val passwordField                   = new PasswordField
    val passwordSuccessNotificationArea = new StackPane

    vBox.setPadding(new Insets(20))
    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setSpacing(10)
    prompt.setStyle("-fx-font-size: 15;")

    vBox.getChildren.addAll(
      prompt,
      passwordField,
      passwordSuccessNotificationArea
    )
    passwordField.setOnAction(_ => {
      passwordAttempt(passwordField, passwordSuccessNotificationArea, passwordSuccess)
    })
    vBox
  }

  private def passwordAttempt(
      passwordField: PasswordField,
      passwordSuccessNotificationArea: StackPane,
      passwordSuccess: => Unit
  ): Unit = {
    Future {
      passwordHelper.testPassword(PasswordAttempt(passwordField.getText)) match {
        case Right(_) =>
          Platform.runLater(() => {
            val correctPassword = new Label("Decrypting")

            correctPassword.setTextFill(Color.rgb(21, 117, 84))
            passwordSuccessNotificationArea.getChildren.removeAll(
              passwordSuccessNotificationArea.getChildren
            )

            passwordSuccessNotificationArea.getChildren.add(correctPassword)
            passwordSuccess
          })

        case Left(error) =>
          Platform.runLater(() => {
            val wrongPassword = new Label(error.uiMessage)
            wrongPassword.setTextFill(Color.rgb(210, 39, 30))
            passwordSuccessNotificationArea.getChildren.removeAll(
              passwordSuccessNotificationArea.getChildren
            )
            passwordSuccessNotificationArea.getChildren.add(wrongPassword)
          })
      }
    }
  }
}
