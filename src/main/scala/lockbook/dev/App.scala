package lockbook.dev

import java.util.concurrent.ScheduledThreadPoolExecutor

import javafx.application.Application
import javafx.stage.Stage

object App {
  val path: String   = s"${System.getProperty("user.home")}/.lockbook"
  val debug: Boolean = true // TODO utilize this to print out exceptions passed into LockbookError

  def main(args: Array[String]) {
    Application.launch(classOf[App], args: _*)
  }
}

class App extends Application {
  override def start(stage: Stage): Unit = {

    val executor: ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2)

    val file: FileHelper                       = new FileHelperImpl
    val encryption: EncryptionHelper           = new EncryptionImpl
    val lockfile: LockfileHelper               = new LockfileHelperImpl(encryption, file)
    val password: PasswordHelper               = new PasswordHelperImpl(lockfile, encryption)
    val gitCredential: GitCredentialHelperImpl = new GitCredentialHelperImpl(encryption, password, file)
    val git: GitHelper                         = new GitHelperImpl(gitCredential, file)
    val editorHelper: EditorHelperImpl         = new EditorHelperImpl(encryption, password, file)
    val settingHelper: SettingHelper           = new SettingHelperImpl(SettingHelper.fromFile)

    val newPasswordUi: NewPasswordUi       = new NewPasswordUi(lockfile, password, encryption)
    val unlockUi: UnlockUi                 = new UnlockUi(password)
    val repositoryCellUi: RepositoryCellUi = new RepositoryCellUi(git)
    val cloneRepoDialog: CloneRepoDialog   = new CloneRepoDialog(git)
    val repositoryUi: RepositoryUi         = new RepositoryUi(git, repositoryCellUi, cloneRepoDialog)
    val fileTreeUi: FileTreeUi             = new FileTreeUi(file)
    val editorUi: EditorUi                 = new EditorUi(editorHelper, git, executor)

    val uiOrchestrator =
      new UiOrchestrator(lockfile, unlockUi, newPasswordUi, repositoryUi, fileTreeUi, editorUi, stage, executor)
    uiOrchestrator.showView()
  }
}
