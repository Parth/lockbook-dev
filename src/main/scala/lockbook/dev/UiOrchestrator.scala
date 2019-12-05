package lockbook.dev

import java.io.File
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.{Menu, MenuBar, MenuItem, SplitPane}
import javafx.scene.input.{KeyCode, KeyCodeCombination, KeyCombination, KeyEvent}
import javafx.scene.layout.{BorderPane, StackPane}
import javafx.stage.{Screen, Stage}
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class UiOrchestrator(
    lockfile: LockfileHelper,
    settingsHelper: SettingsHelper,
    settingsUi: SettingsUi,
    unlockUI: UnlockUi,
    newPassphraseUI: NewPassphraseUi,
    repositoryUi: RepositoryUi,
    fileTreeUi: FileTreeUi,
    editorUi: EditorUi,
    stage: Stage,
    executor: ScheduledThreadPoolExecutor
) {

  private var locked: Boolean  = false // Give this some more thought
  private var closing: Boolean = false

  def showView(): Unit = {
    val borderPane           = new BorderPane // revert back to root
    val stackPane: StackPane = new StackPane
    borderPane.setCenter(stackPane)
    stage.setMaximized(false)
    stage.setFullScreen(false)
    stage.setScene(new Scene(borderPane, 300, 130))
    stage.setTitle("Lockbook Dev")
    stage.getScene.getStylesheets.add("light.css")
    processLockfileAndShowUi(stackPane, showRepo())
    stage.show()
  }

  private def getMenuUi: MenuBar = {
    val menuBar      = new MenuBar
    val fileMenu     = new Menu("File")
    val settingsItem = new MenuItem("Settings")

    settingsItem.setOnAction(_ => {
      settingsUi.showView
    })

    fileMenu.getItems.add(settingsItem)
    menuBar.getMenus.add(fileMenu)

    menuBar
  }

  private def showRepo(): Unit = {
    stage.close()
    locked = false

    val splitPane       = new SplitPane
    val borderPane      = new BorderPane
    val repoStackPane   = new StackPane
    val fileStackPane   = new StackPane
    val editorStackPane = new StackPane

    borderPane.setTop(getMenuUi)
    borderPane.setCenter(splitPane)

    splitPane.setDividerPositions(0.15, 0.3)
    splitPane.getItems.setAll(repoStackPane, fileStackPane, editorStackPane)
    repoStackPane.getChildren.setAll(
      repositoryUi.getView(showFileUi(fileStackPane, editorStackPane))
    )

    stage.setScene(
      new Scene(
        borderPane,
        Screen.getPrimary.getVisualBounds.getWidth * 0.8,
        Screen.getPrimary.getVisualBounds.getHeight * 0.8
      )
    )

    closeRequestListener()
    addFocusListener()
    stage.show()
  }

  private def showFileUi(fileContainer: StackPane, editorContainer: StackPane)(repo: Git): Unit = {
    fileContainer.getChildren.add(fileTreeUi.getView(repo, showEditorUi(editorContainer)))
  }

  private def showEditorUi(container: StackPane)(git: Git, f: File): Unit = {
    container.getChildren.setAll(editorUi.getView(git, f))
  }

  private def processLockfileAndShowUi(root: StackPane, onDone: => Unit): Unit = {
    Future {
      lockfile.getLockfile match {
        case Right(_) =>
          Platform.runLater(() => root.getChildren.add(unlockUI.getView(onDone)))
        case Left(_) =>
          Platform.runLater(() => root.getChildren.add(newPassphraseUI.getView(onDone)))
      }
    }
  }

  private def closeRequestListener(): Unit =
    stage.setOnCloseRequest(_ => {
      closing = true
      executor.shutdownNow()
    })

  private def addFocusListener(): Unit = {
    val lockWhenBackground =
      CancelableAction(executor, FiniteDuration(15, TimeUnit.MINUTES), lockTask) // good settings candidate

    var refreshRepos: Option[ScheduledFuture[_]] = Some(
      executor.scheduleAtFixedRate(refreshStatus, 1, 1, TimeUnit.SECONDS)
    )

    stage
      .focusedProperty()
      .addListener((_, isHidden, _) => {
        if (!locked && !closing) {
          lockWhenBackground.cancel()
          if (isHidden) {
            refreshRepos.map(_.cancel(false))
            refreshRepos = None
            lockWhenBackground.schedule()
          }
        }

        if (!locked && !closing && !isHidden) {
          if (refreshRepos.isEmpty || refreshRepos.get.isCancelled) {
            refreshRepos = Some(executor.scheduleAtFixedRate(refreshStatus, 1, 1, TimeUnit.SECONDS))
          }
        }
      })

    val saveKeyCombo = new KeyCodeCombination(KeyCode.L, KeyCombination.META_DOWN)

    stage.getScene
      .addEventHandler(
        KeyEvent.KEY_PRESSED,
        (event: KeyEvent) => {
          if (saveKeyCombo.`match`(event)) {
            refreshRepos.map(_.cancel(false))
            refreshRepos = None
            lockWhenBackground.doNow()
          }
        }
      )
  }

  private val lockTask: () => Unit = () => {
    Platform.runLater(() => {
      locked = true
      stage.close()
      showView()
    })
  }

  private val refreshStatus = new Runnable {
    def run(): Unit = repositoryUi.setRepoStatuses()
  }
}
