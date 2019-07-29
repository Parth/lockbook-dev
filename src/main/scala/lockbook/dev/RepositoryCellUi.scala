package lockbook.dev

import javafx.scene.control._
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

  def getListCell(
      onClick: Git => Unit,
      onDelete: RepositoryCell => Unit
  ): ListCell[RepositoryCell] = {

    val listCell: ListCell[RepositoryCell] = new ListCell[RepositoryCell]() {

      // TODO perhaps this can be smaller: https://stackoverflow.com/questions/28264907/javafx-listview-contextmenu
      override def updateItem(item: RepositoryCell, empty: Boolean): Unit = {
        super.updateItem(item, empty)

        if (empty || item == null) setText(null)
        else {
          setGraphic(getCell(item))
          val deleteItem = new MenuItem("Delete")
          deleteItem.setOnAction(_ => onDelete(item))
          setContextMenu(new ContextMenu(deleteItem))
        }
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
