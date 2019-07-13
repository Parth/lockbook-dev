package devbook

import javafx.scene.control.SplitPane
import javafx.scene.{Node, Scene}
import javafx.stage.Stage

class UiOrchestrator(lockfile: Lockfile, unlockUI: UnlockUi, newPasswordUI: NewPasswordUi) {

  private val root: SplitPane = new SplitPane

  def showView(stage: Stage): Unit = {
    stage.setTitle("Devbook")

    root.getItems.add(showLogin)

    stage.setScene(new Scene(root, 300, 600))
    stage.show()
  }

  def showLogin: Node = {
    lockfile.getLockfile match {
      case Some(_) =>
        unlockUI.getView(() => println("done"))
      case None =>
        newPasswordUI.getView
    }
  }
}
