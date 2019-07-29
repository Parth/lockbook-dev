package lockbook.dev

import javafx.application.Platform
import javafx.scene.control.ProgressIndicator
import org.eclipse.jgit.lib.ProgressMonitor

class PullProgressMonitor(val progressIndicator: ProgressIndicator) extends ProgressMonitor {

  var startedTasks                          = 0
  var completedTasks                        = 0
  var progress                              = 0.0
  var total                                 = 0.0
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
    Platform.runLater(() => progressIndicator.setProgress(progress / total))
  }

  override def endTask(): Unit = {
    println("endtask")

    completedTasks += 1
    if (completedTasks == startedTasks)
      Platform.runLater(() => progressIndicator.setProgress(1))
  }

  override def isCancelled: Boolean = false
}
