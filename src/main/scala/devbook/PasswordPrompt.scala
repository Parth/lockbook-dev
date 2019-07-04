package devbook

import javafx.scene.Scene
import javafx.scene.control.PasswordField
import javafx.scene.layout.HBox
import javafx.stage.Stage

trait PasswordPrompt {
  def promptUserForPassword(stage: Stage): Unit
  def getValidatedPassword: Option[String]
}

class PasswordPromptImpl(repositoryList: RepositoryList)
    extends PasswordPrompt {
  var password: Option[String] = None

  override def promptUserForPassword(stage: Stage): Unit = {
    val root = new HBox
    val passwordField = new PasswordField

    passwordField.setOnAction(event => {
      password = Some(passwordField.getText)
      stage.close()
    })

    root.getChildren.add(passwordField)

    stage.setScene(new Scene(root, 300, 250))
    stage.show()
  }

  override def getValidatedPassword: Option[String] = ???
}
