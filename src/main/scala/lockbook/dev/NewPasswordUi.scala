package lockbook.dev

import javafx.geometry.Pos
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color

class NewPasswordUi(
    lockfile: LockfileHelper,
    passwordHelper: PasswordHelper,
    encryptionHelper: EncryptionHelper
) {

  def getView(onDone: => Unit): VBox = {
    val vBox                 = new VBox
    val header               = new Label("Create a new Lockbook")
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

      passwordHelper
        .passwordIfMatch(password1, password2)
        .map(passwordHelper.setPassword)
        .flatMap(encryptionHelper.encrypt(DecryptedValue("unlocked"), _))
        .flatMap(lockfile.writeToLockfile) match {

        case Left(error) =>
          val wrongPassword = new Label(error.uiMessage)
          wrongPassword.setTextFill(Color.rgb(210, 39, 30))
          attemptInfoArea.getChildren.removeAll(
            attemptInfoArea.getChildren
          )
          attemptInfoArea.getChildren.add(wrongPassword)

        case Right(_) =>
          val correctPassword = new Label("Password Set")
          correctPassword.setTextFill(Color.rgb(21, 117, 84))
          attemptInfoArea.getChildren.removeAll(
            attemptInfoArea.getChildren
          )
          attemptInfoArea.getChildren.add(correctPassword)
          onDone
      }
    })

    vBox
  }
}
