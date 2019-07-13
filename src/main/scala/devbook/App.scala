package devbook

import javafx.application.Application
import javafx.stage.Stage

object App {

  def main(args: Array[String]) {
    Application.launch(classOf[App], args: _*)
  }
}

class App extends Application {
  override def start(primaryStage: Stage): Unit = {
    val encryptionHelper: EncryptionHelper = new EncryptionImpl
    val lockfile: Lockfile                 = new LockfileImpl(encryptionHelper)
    val passwordHelper: PasswordHelper     = new PasswordHelperImpl(lockfile, encryptionHelper)

    val newPasswordUi = new NewPasswordUi(lockfile)
    val unlockUi      = new UnlockUi(passwordHelper)
    val repositoryUi  = new RepositoryUi()

    val uiOrchestrator = new UiOrchestrator(lockfile, unlockUi, newPasswordUi, repositoryUi)
    uiOrchestrator.showView(primaryStage)
  }
}
