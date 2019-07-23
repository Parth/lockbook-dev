package devbook

import java.io.File

import javafx.scene.control.SplitPane
import javafx.scene.layout.StackPane
import javafx.scene.{Node, Scene}
import javafx.stage.{Screen, Stage}
import org.eclipse.jgit.api.Git

import scala.util.{Failure, Success}

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
    root.getChildren.add(showLogin(showRepo(stage)))
    stage.setScene(new Scene(root, 300, 100))
    stage.getScene.getStylesheets.add("dark.css")
    stage.setTitle("Lockbook Dev")
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

    stage.getScene.getStylesheets.add("dark.css")
    stage.show()
  }

  def showFileUi(fileContainer: StackPane, editorContainer: StackPane)(repo: Git): Unit = {
    fileContainer.getChildren.add(fileTreeUi.getView(repo, showEditorUi(editorContainer)))
  }

  def showEditorUi(container: StackPane)(git: Git, f: File): Unit = {
    container.getChildren.setAll(editorUi.getView(git, f))
  }

  def showLogin(onDone: => Unit): Node = {
    lockfile.getLockfile match {
      case Success(_) =>
        unlockUI.getView(onDone)
      case Failure(_) =>
        newPasswordUI.getView(onDone)
    }
  }
}
