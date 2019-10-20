package lockbook.dev

import java.io.File
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.SplitPane
import javafx.scene.input.{KeyCode, KeyCodeCombination, KeyCombination, KeyEvent}
import javafx.scene.layout.StackPane
import javafx.stage.{Screen, Stage}
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class UiOrchestrator(
    lockfile: LockfileHelper,
    unlockUI: UnlockUi,
    newPasswordUI: NewPasswordUi,
    repositoryUi: RepositoryUi,
    fileTreeUi: FileTreeUi,
    editorUi: EditorUi,
    stage: Stage,
    executor: ScheduledThreadPoolExecutor
) {

  private var locked: Boolean  = false // Give this some more thought
  private var closing: Boolean = false

  def showView(): Unit = {
    val root: StackPane = new StackPane
    stage.setMaximized(false)
    stage.setFullScreen(false)
    stage.setScene(new Scene(root, 300, 130))
    stage.setTitle("Lockbook Dev")
    stage.getScene.getStylesheets.add("light.css")
    processLockfileAndShowUi(root, showRepo())
    stage.show()
  }

  private def showRepo(): Unit = {
    stage.close()
    locked = false

    val root            = new SplitPane
    val repoStackPane   = new StackPane
    val fileStackPane   = new StackPane
    val editorStackPane = new StackPane

    root.setDividerPositions(0.15, 0.3)
    root.getItems.setAll(repoStackPane, fileStackPane, editorStackPane)
    repoStackPane.getChildren.setAll(
      repositoryUi.getView(showFileUi(fileStackPane, editorStackPane))
    )

    stage.setScene(
      new Scene(
        root,
        Screen.getPrimary.getVisualBounds.getWidth * 0.8,
        Screen.getPrimary.getVisualBounds.getHeight * 0.8
      )
    )

    stage.getScene.getStylesheets.add("light.css")    // good settings candidate
    stage.getScene.getStylesheets.add("markdown.css") // good settings candidate
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
          Platform.runLater(() => root.getChildren.add(newPasswordUI.getView(onDone)))
      }
    }
  }

  private def closeRequestListener(): Unit = {
    stage.setOnCloseRequest(_ => {
      closing = true
      executor.shutdown()
    })
  }

  private def addFocusListener(): Unit = {
    val lockWhenBackground =
      CancelableAction(executor, FiniteDuration(5, TimeUnit.MINUTES), lockTask) // good settings candidate

    stage
      .focusedProperty()
      .addListener((_, isHidden, _) => {
        if (!locked && !closing) {
          lockWhenBackground.cancel()
          if (isHidden) {
            lockWhenBackground.schedule()
          }
        }

        if (!locked && !closing && !isHidden) {
          repositoryUi.pullAllRepos()
        }
      })

    val saveKeyCombo = new KeyCodeCombination(KeyCode.L, KeyCombination.META_DOWN)

    stage.getScene
      .addEventHandler(
        KeyEvent.KEY_PRESSED,
        (event: KeyEvent) => {
          if (saveKeyCombo.`match`(event)) {
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

}
