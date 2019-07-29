package lockbook.dev

import javafx.application.Platform
import javafx.scene.control.{Label, ListCell, ProgressIndicator}
import javafx.scene.layout.GridPane
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor

class RepositoryCellUi(gitHelper: GitHelper) {

  def getListCell(onClick: Git => Unit): ListCell[Git] = {

    val listCell: ListCell[Git] = new ListCell[Git]() {
      override def updateItem(item: Git, empty: Boolean): Unit = {
        super.updateItem(item, empty)

        if (empty || item == null) setText(null)
        else
          setGraphic(getCell(item))
      }
    }

    // TODO: This perhaps isn't Cell's responsibility, there's an onchange, that cal also get rid of needless state changes
    listCell.setOnMouseClicked(_ => {
      if (!listCell.isEmpty) {
        onClick(listCell.getItem)
      }
    })
    listCell
  }

  private def getCell(git: Git): GridPane = {
    val gridPane          = new GridPane
    val label             = new Label(gitHelper.getRepoName(git))
    val progressIndicator = new ProgressIndicator(0)
    val progressMonitor   = cellProgressMonitor(progressIndicator)

    gitHelper.pull(git, progressMonitor)

    gridPane.add(label, 0, 0)
    gridPane.add(progressIndicator, 1, 0)

    gridPane
  }

  private def cellProgressMonitor(progressIndicator: ProgressIndicator): ProgressMonitor = {

    var startedTasks   = 0
    var completedTasks = 0
    var progress       = 0.0
    var total          = 0.0

    new ProgressMonitor {
      override def start(totalTasks: Int): Unit = {}

      override def beginTask(title: String, totalWork: Int): Unit = {
        println(s"beginTask, $title, $totalWork")
        total += totalWork
        startedTasks += 1
        Platform.runLater(() => {
          progressIndicator.setProgress(progress / total)
        })
      }

      override def update(completed: Int): Unit = {
        println(s"update, $completed")
        progress += completed
        Platform.runLater(() => progressIndicator.setProgress(completed / total))
      }

      override def endTask(): Unit = {
        println("endtask")

        completedTasks += 1
        if (completedTasks == startedTasks)
          Platform.runLater(() => progressIndicator.setProgress(1))
      }

      override def isCancelled: Boolean = false
    }
  }

}
