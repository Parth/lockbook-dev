package devbook

import javafx.geometry.Pos
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration._

class PasswordUI(primaryStream: Subject[Events], lockfile: EncryptedValue) {
  val vBox = new VBox
  val passwordField = new PasswordField
  val passwordSuccessNotificationArea = new StackPane

  def getView: VBox = {
    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setSpacing(10)
    vBox.getChildren.addAll(passwordField, passwordSuccessNotificationArea)
    setupListeners()
    vBox
  }

  private def setupListeners(): Unit = {
    passwordField.setOnAction(_ => {
      primaryStream.onNext(
        PasswordAttempt(
          passwordField.getText,
          lockfile
        )
      )
    })

    primaryStream
      .collect { case attempt: PasswordAttempt => attempt }
      .map(Encryption.testPassword)
      .subscribe(either => {
        either match {
          case Left(pw)  => passwordSuccess(pw)
          case Right(pf) => passwordFailed(pf)
        }
      })
  }

  private def passwordSuccess(passwordSuccess: PasswordSuccess): Unit = {
    val correctPassword = new Label("Decrypting")
    correctPassword.setTextFill(Color.rgb(21, 117, 84))
    passwordSuccessNotificationArea.getChildren.removeAll(
      passwordSuccessNotificationArea.getChildren
    )

    passwordSuccessNotificationArea.getChildren.add(correctPassword)
    Observable
      .timer(Duration(1, SECONDS))
      .subscribe(_ => {
        primaryStream.onNext(ShowRepository())
      })
  }

  private def passwordFailed(passwordFailure: PasswordFailure): Unit = {
    val wrongPassword = new Label(passwordFailure.message)
    wrongPassword.setTextFill(Color.rgb(210, 39, 30))
    passwordSuccessNotificationArea.getChildren.removeAll(
      passwordSuccessNotificationArea.getChildren
    )
    passwordSuccessNotificationArea.getChildren.add(wrongPassword)
  }
}
