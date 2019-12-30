package lockbook.dev

import java.util.concurrent.ScheduledThreadPoolExecutor

import javafx.application.Application
import javafx.stage.Stage

object App {
  val path: String   = s"${System.getProperty("user.home")}/.lockbook"
  val debug: Boolean = true // TODO utilize this to print out exceptions passed into LockbookError
  val css: String    = "light.css"

  def main(args: Array[String]) =
    Application.launch(classOf[App], args: _*)
}

class App extends Application {

  def addCss(stage: Stage, settingsHelper: SettingsHelper): Unit =
    stage
      .sceneProperty()
      .addListener((_, _, newValue) => {
        if (newValue != null) {
          newValue.getStylesheets.addAll(settingsHelper.getTheme.fileName, "markdown.css")
        }
      })

  override def start(stage: Stage): Unit = {

    val executor: ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2)

    val file: FileHelper               = new FileHelperImpl
    val settingsHelper: SettingsHelper = new SettingsHelperImpl(SettingsHelper.fromFile(file))

    addCss(stage, settingsHelper)

    val encryption: EncryptionHelper = new EncryptionImpl
    val lockfile: LockfileHelper     = new LockfileHelperImpl(encryption, file)
    val passphrase: PassphraseHelper = new PassphraseHelperImpl(lockfile, encryption)
    val gitCredential: GitCredentialHelperImpl =
      new GitCredentialHelperImpl(encryption, passphrase, settingsHelper, file)
    val git: GitHelper                 = new GitHelperImpl(gitCredential, file)
    val editorHelper: EditorHelperImpl = new EditorHelperImpl(encryption, passphrase, file)

    val newPassphraseUi: NewPassphraseUi   = new NewPassphraseUi(lockfile, passphrase, encryption)
    val settingsUi: SettingsUi             = new SettingsUi(settingsHelper, file)
    val unlockUi: UnlockUi                 = new UnlockUi(passphrase)
    val repositoryCellUi: RepositoryCellUi = new RepositoryCellUi(git)
    val cloneRepoDialog: CloneRepoDialog   = new CloneRepoDialog(git)
    val repositoryUi: RepositoryUi         = new RepositoryUi(git, repositoryCellUi, cloneRepoDialog)
    val fileTreeUi: FileTreeUi             = new FileTreeUi(file)
    val editorUi: EditorUi                 = new EditorUi(editorHelper, git, executor)

    val uiOrchestrator =
      new UiOrchestrator(
        lockfile,
        settingsHelper,
        settingsUi,
        unlockUi,
        newPassphraseUi,
        repositoryUi,
        fileTreeUi,
        editorUi,
        stage,
        executor
      )
    uiOrchestrator.showView()
  }
}
