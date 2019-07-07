package devbook

import javafx.geometry.Pos
import javafx.scene.control.{Label, PasswordField}
import javafx.scene.layout._
import javafx.scene.paint.Color
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration._

class NewDevbookUI(primaryStream: Subject[Events]) {

  val vBox = new VBox

  val header = new Label("Create a new Devbook")
  val passwordPrompt = new Label("Set a password")
  val passwordField = new PasswordField
  val confirmPasswordField = new PasswordField

  val passwordSuccessNotificationArea = new StackPane

  def getView: VBox = {
    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setSpacing(10)
    vBox.getChildren.addAll(
      header,
      passwordField,
      confirmPasswordField,
      passwordSuccessNotificationArea
    )
    vBox
  }

  def setupListeners(): Unit = {
    confirmPasswordField.setOnAction(_ => {
      val password1 = passwordField.getText
      val password2 = confirmPasswordField.getText

      if (password1 == password2) { // TODO Enforce some password requirements
        Lockfile.createLockfile(PasswordSuccess(password1)) match {
          case Some(error) =>
            primaryStream.onNext(PasswordCreationFailure(error.toString))
          case None =>
            primaryStream.onNext(PasswordCreationSuccess(password1))
        }
      } else {
        primaryStream.onNext(PasswordFailure("Passwords do not match"))
      }
    })

    primaryStream
      .collect { case success: PasswordCreationSuccess => success }
      .subscribe(passwordSuccess _)

    primaryStream
      .collect { case failure: PasswordCreationFailure => failure }
      .subscribe(passwordFailed _)
  }

  private def passwordSuccess(passwordSuccess: PasswordCreationSuccess): Unit = {
    val correctPassword = new Label("Password Set")
    correctPassword.setTextFill(Color.rgb(21, 117, 84))
    passwordSuccessNotificationArea.getChildren.removeAll(
      passwordSuccessNotificationArea.getChildren
    )

    passwordSuccessNotificationArea.getChildren.add(correctPassword)
    primaryStream.onNext(PasswordSuccess(passwordSuccess.password))
    primaryStream.onNext(ShowRepository())
  }

  private def passwordFailed(passwordFailure: PasswordCreationFailure): Unit = { // https://stackoverflow.com/questions/21083945/how-to-avoid-not-on-fx-application-thread-currentthread-javafx-application-th ?
    val wrongPassword = new Label(passwordFailure.message)
    wrongPassword.setTextFill(Color.rgb(210, 39, 30))
    passwordSuccessNotificationArea.getChildren.removeAll(
      passwordSuccessNotificationArea.getChildren
    )
    passwordSuccessNotificationArea.getChildren.add(wrongPassword)
  }
}
