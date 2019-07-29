package lockbook.dev

import javafx.scene.control.{Label, ListCell, ProgressIndicator}
import javafx.scene.layout.GridPane
import org.eclipse.jgit.api.{Git, PullCommand}

case class RepositoryCell(git: Git, pullCommand: PullCommand, progressMonitor: PullProgressMonitor)

object RepositoryCell {
  def fromGit(git: Git, gitHelper: GitHelper): RepositoryCell = {
    val ppm = new PullProgressMonitor(new ProgressIndicator(0))
    RepositoryCell(
      git = git,
      pullCommand = gitHelper.pullCommand(git, ppm),
      progressMonitor = ppm
    )
  }
}

class RepositoryCellUi(gitHelper: GitHelper) {

  def getListCell(onClick: Git => Unit): ListCell[RepositoryCell] = {

    val listCell: ListCell[RepositoryCell] = new ListCell[RepositoryCell]() {

      override def updateItem(item: RepositoryCell, empty: Boolean): Unit = {
        super.updateItem(item, empty)

        if (empty || item == null) setText(null)
        else
          setGraphic(getCell(item))
      }
    }

    listCell
  }

  private def getCell(repositoryCell: RepositoryCell): GridPane = {
    val gridPane = new GridPane
    val label    = new Label(gitHelper.getRepoName(repositoryCell.git))

    gridPane.add(label, 0, 0)
    gridPane.add(repositoryCell.progressMonitor.progressIndicator, 1, 0)

    gridPane
  }
}
