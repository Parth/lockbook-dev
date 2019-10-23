package lockbook.dev

import javafx.geometry.Pos
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color

class NewPassphraseUi(
    lockfile: LockfileHelper,
    passphraseHelper: PassphraseHelper,
    encryptionHelper: EncryptionHelper
) {

  def getView(onDone: => Unit): VBox = {
    val vBox                   = new VBox
    val header                 = new Label("Set Lockbook Passphrase")
    val passphraseField        = new PasswordField
    val confirmPassphraseField = new PasswordField
    val attemptInfoArea        = new StackPane

    passphraseField.setPromptText("Enter passphrase")
    confirmPassphraseField.setPromptText("Confirm passphrase")

    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setSpacing(10)
    vBox.getChildren.addAll(
      header,
      passphraseField,
      confirmPassphraseField,
      attemptInfoArea
    )

    confirmPassphraseField.setOnAction(_ => {
      val passphrase1 = passphraseField.getText
      val passphrase2 = confirmPassphraseField.getText

      passphraseHelper
        .passphraseIfMatch(passphrase1, passphrase2)
        .map(passphraseHelper.setPassphrase)
        .flatMap(encryptionHelper.encrypt(DecryptedValue("unlocked"), _))
        .flatMap(lockfile.writeToLockfile) match {

        case Left(error) =>
          val wrongPassphrase = new Label(error.uiMessage)
          wrongPassphrase.setTextFill(Color.rgb(210, 39, 30))
          attemptInfoArea.getChildren.removeAll(
            attemptInfoArea.getChildren
          )
          attemptInfoArea.getChildren.add(wrongPassphrase)

        case Right(_) =>
          val correctPassphrase = new Label("Passphrase Set")
          correctPassphrase.setTextFill(Color.rgb(21, 117, 84))
          attemptInfoArea.getChildren.removeAll(
            attemptInfoArea.getChildren
          )
          attemptInfoArea.getChildren.add(correctPassphrase)
          onDone
      }
    })

    vBox
  }
}
