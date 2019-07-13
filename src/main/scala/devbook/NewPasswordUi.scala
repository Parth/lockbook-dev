package devbook

import javafx.geometry.Pos
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color

class NewPasswordUi(lockfile: Lockfile) {

  def getView(onDone: => Unit): VBox = {
    val vBox                 = new VBox
    val header               = new Label("Create a new Devbook")
    val passwordField        = new PasswordField
    val confirmPasswordField = new PasswordField
    val attemptInfoArea      = new StackPane

    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setSpacing(10)
    vBox.getChildren.addAll(
      header,
      passwordField,
      confirmPasswordField,
      attemptInfoArea
    )

    confirmPasswordField.setOnAction(_ => {
      val password1 = passwordField.getText
      val password2 = confirmPasswordField.getText

      if (password1 == password2) { // TODO calculate brute force cost
        lockfile.createLockfile(Password(password1)) match {
          case Some(error) =>
            val wrongPassword = new Label(error.toString)
            wrongPassword.setTextFill(Color.rgb(210, 39, 30))
            attemptInfoArea.getChildren.removeAll(
              attemptInfoArea.getChildren
            )
            attemptInfoArea.getChildren.add(wrongPassword)

          case None =>
            val correctPassword = new Label("Password Set")
            correctPassword.setTextFill(Color.rgb(21, 117, 84))
            attemptInfoArea.getChildren.removeAll(
              attemptInfoArea.getChildren
            )
            attemptInfoArea.getChildren.add(correctPassword)

            onDone
        }
      } else {
        val wrongPassword = new Label("Passwords do not match")
        wrongPassword.setTextFill(Color.rgb(210, 39, 30))
        attemptInfoArea.getChildren.removeAll(
          attemptInfoArea.getChildren
        )
        attemptInfoArea.getChildren.add(wrongPassword)
      }
    })

    vBox
  }
}
