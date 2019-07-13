package devbook

import javafx.geometry.Pos
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color

class UnlockUi(passwordHelper: PasswordHelper) {
  def getView(passwordSuccess: => Unit): VBox = {
    val vBox                            = new VBox
    val passwordField                   = new PasswordField
    val passwordSuccessNotificationArea = new StackPane
    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setSpacing(10)
    vBox.getChildren.addAll(passwordField, passwordSuccessNotificationArea)
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
    passwordHelper.testPassword(PasswordAttempt(passwordField.getText)) match {
      case Left(_) =>
        val correctPassword = new Label("Decrypting")
        correctPassword.setTextFill(Color.rgb(21, 117, 84))
        passwordSuccessNotificationArea.getChildren.removeAll(
          passwordSuccessNotificationArea.getChildren
        )

        passwordSuccessNotificationArea.getChildren.add(correctPassword)
        passwordSuccess

      case Right(error) =>
        val wrongPassword = new Label(error.toString)
        wrongPassword.setTextFill(Color.rgb(210, 39, 30))
        passwordSuccessNotificationArea.getChildren.removeAll(
          passwordSuccessNotificationArea.getChildren
        )
        passwordSuccessNotificationArea.getChildren.add(wrongPassword)
    }
  }
}
