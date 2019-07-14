package devbook

import java.io.File

import javafx.scene.control.SplitPane
import javafx.scene.{Node, Scene}
import javafx.stage.Stage
import org.eclipse.jgit.api.Git

class UiOrchestrator(
    lockfile: Lockfile,
    unlockUI: UnlockUi,
    newPasswordUI: NewPasswordUi,
    repositoryUi: RepositoryUi,
    fileTreeUi: FileTreeUi
) {

  def showView(stage: Stage): Unit = {
    val root: SplitPane = new SplitPane
    root.getItems.add(showLogin(showRepo(stage, root)))

    stage.setScene(new Scene(root, 300, 600))
    stage.setTitle("Lockbook Dev")
    stage.show()
  }

  def showRepo(stage: Stage, root: SplitPane): Unit = {
    root.getItems.removeAll(root.getItems)
    root.getItems.add(repositoryUi.getView(showFileUi(stage, root)))
  }

  def showFileUi(stage: Stage, root: SplitPane)(repo: Git): Unit = {
    stage.setWidth(stage.getWidth * 2)
    root.getItems.add(fileTreeUi.getView(repo, showEditorUi(stage, root))) // TODO only do this once
    root.setDividerPositions(0.4, 0.6)
  }

  def showEditorUi(stage: Stage, root: SplitPane)(f: File): Unit = {
    stage.setWidth(stage.getWidth * 2)
    root.setDividerPositions(0.3, 0.3, 0.4)
  }

  def showLogin(onDone: => Unit): Node = {
    lockfile.getLockfile match {
      case Some(_) =>
        unlockUI.getView(onDone)
      case None =>
        newPasswordUI.getView(onDone)
    }
  }
}
