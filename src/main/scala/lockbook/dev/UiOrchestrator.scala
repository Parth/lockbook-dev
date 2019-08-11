package lockbook.dev

import java.io.File

import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.SplitPane
import javafx.scene.layout.StackPane
import javafx.stage.{Screen, Stage}
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UiOrchestrator(
    lockfile: LockfileHelper,
    unlockUI: UnlockUi,
    newPasswordUI: NewPasswordUi,
    repositoryUi: RepositoryUi,
    fileTreeUi: FileTreeUi,
    editorUi: EditorUi
) {

  def showView(stage: Stage): Unit = {
    val root: StackPane = new StackPane
    stage.setScene(new Scene(root, 300, 130))
    stage.setTitle("Lockbook Dev")
    processLockfileAndShowUi(root, showRepo(stage))
    stage.show()
  }

  def showRepo(stage: Stage): Unit = {
    stage.close()

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

    stage.show()
  }

  def showFileUi(fileContainer: StackPane, editorContainer: StackPane)(repo: Git): Unit = {
    fileContainer.getChildren.add(fileTreeUi.getView(repo, showEditorUi(editorContainer)))
  }

  def showEditorUi(container: StackPane)(git: Git, f: File): Unit = {
    container.getChildren.setAll(editorUi.getView(git, f))
  }

  def processLockfileAndShowUi(root: StackPane, onDone: => Unit): Unit = {
    Future {
      lockfile.getLockfile match {
        case Right(_) =>
          Platform.runLater(() => root.getChildren.add(unlockUI.getView(onDone)))

        case Left(_) =>
          Platform.runLater(() => root.getChildren.add(newPasswordUI.getView(onDone)))
      }
    }
  }
}
