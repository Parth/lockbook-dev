package lockbook.dev

import javafx.application.Application
import javafx.stage.Stage

object App {
  val path = s"${System.getProperty("user.home")}/.lockbook"

  def main(args: Array[String]) {
    Application.launch(classOf[App], args: _*)
  }
}

class App extends Application {
  override def start(primaryStage: Stage): Unit = {

    val file: FileHelper             = new FileHelperImpl
    val encryption: EncryptionHelper = new EncryptionImpl
    val lockfile: LockfileHelper     = new LockfileHelperImpl(encryption, file)
    val password: PasswordHelper     = new PasswordHelperImpl(lockfile, encryption)
    val gitCredential                = new GitCredentialHelperImpl(encryption, password, file)
    val git: GitHelper               = new GitHelperImpl(gitCredential)
    val editorHelper                 = new EditorHelperImpl(encryption, password, git, file)

    val newPasswordUi = new NewPasswordUi(lockfile, password, encryption)
    val unlockUi      = new UnlockUi(password)
    val repositoryUi  = new RepositoryUi(git)
    val fileTreeUi    = new FileTreeUi
    val editorUi      = new EditorUi(editorHelper)

    val uiOrchestrator =
      new UiOrchestrator(lockfile, unlockUi, newPasswordUi, repositoryUi, fileTreeUi, editorUi)
    uiOrchestrator.showView(primaryStage)
  }
}
