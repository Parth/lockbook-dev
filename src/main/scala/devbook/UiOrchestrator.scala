package devbook

import javafx.scene.control.SplitPane
import javafx.scene.{Node, Scene}
import javafx.stage.Stage

class UiOrchestrator(
    lockfile: Lockfile,
    unlockUI: UnlockUi,
    newPasswordUI: NewPasswordUi,
    repositoryUi: RepositoryUi
) {

  private val root: SplitPane = new SplitPane

  def showView(stage: Stage): Unit = {
    stage.setTitle("Devbook")

    root.getItems.add(showLogin(showRepo))

    stage.setScene(new Scene(root, 300, 600))
    stage.show()
  }

  def showRepo: Unit = {
    root.getItems.removeAll(root.getItems)
    root.getItems.add(repositoryUi.getView)
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
