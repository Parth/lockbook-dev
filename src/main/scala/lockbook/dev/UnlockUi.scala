package lockbook.dev

import javafx.application.Platform
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UnlockUi(passphraseHelper: PassphraseHelper) {
  def getView(passphraseSuccess: => Unit): VBox = {

    val vBox                              = new VBox
    val prompt                            = new Label("Enter your passphrase")
    val passphraseField                   = new PasswordField
    val passphraseSuccessNotificationArea = new StackPane

    passphraseField.setPromptText("Enter passphrase")

    vBox.setPadding(new Insets(20))
    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setSpacing(10)
    prompt.setStyle("-fx-font-size: 15;")

    vBox.getChildren.addAll(
      prompt,
      passphraseField,
      passphraseSuccessNotificationArea
    )
    passphraseField.setOnAction(_ => {
      passphraseAttempt(passphraseField, passphraseSuccessNotificationArea, passphraseSuccess)
    })
    vBox
  }

  private def passphraseAttempt(
      passphraseField: PasswordField,
      passphraseSuccessNotificationArea: StackPane,
      passphraseSuccess: => Unit
  ): Unit = {
    Future {
      passphraseHelper.testAndSetPassphrase(PassphraseAttempt(passphraseField.getText)) match {
        case Right(_) =>
          Platform.runLater(() => {
            val correctPassphrase = new Label("Decrypting")

            correctPassphrase.setTextFill(Color.rgb(21, 117, 84))
            passphraseSuccessNotificationArea.getChildren.removeAll(
              passphraseSuccessNotificationArea.getChildren
            )

            passphraseSuccessNotificationArea.getChildren.add(correctPassphrase)
            passphraseSuccess
          })

        case Left(error) =>
          Platform.runLater(() => {
            val wrongPassphrase = new Label(error.uiMessage)
            wrongPassphrase.setTextFill(Color.rgb(210, 39, 30))
            passphraseSuccessNotificationArea.getChildren.removeAll(
              passphraseSuccessNotificationArea.getChildren
            )
            passphraseSuccessNotificationArea.getChildren.add(wrongPassphrase)
          })
      }
    }
  }
}
