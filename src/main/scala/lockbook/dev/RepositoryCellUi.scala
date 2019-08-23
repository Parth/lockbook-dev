package lockbook.dev

import javafx.geometry.HPos
import javafx.scene.control._
import javafx.scene.layout.{ColumnConstraints, GridPane, Priority}
import org.eclipse.jgit.api.{Git, PullCommand}

case class RepositoryCell(git: Git, pullCommand: PullCommand, progressMonitor: PullProgressMonitor)

object RepositoryCell {
  def fromGit(git: Git, gitHelper: GitHelper): RepositoryCell = {
    val ppm = new PullProgressMonitor(getProgressIndicator)
    RepositoryCell(
      git = git,
      pullCommand = gitHelper.pullCommand(git, ppm),
      progressMonitor = ppm
    )
  }

  private def getProgressIndicator: ProgressIndicator = {
    val progressIndicator = new ProgressIndicator(0)
    progressIndicator
      .progressProperty()
      .addListener(_ => {
        if (progressIndicator.getProgress == 1) progressIndicator.setVisible(false)
      })
    progressIndicator
  }
}

class RepositoryCellUi(gitHelper: GitHelper) {

  def getListCell(
      onClick: Git => Unit,
      onDelete: RepositoryCell => Unit
  ): ListCell[RepositoryCell] = {

    new ListCell[RepositoryCell]() {

      // TODO perhaps this can be smaller: https://stackoverflow.com/questions/28264907/javafx-listview-contextmenu
      override def updateItem(item: RepositoryCell, empty: Boolean): Unit = {
        super.updateItem(item, empty)

        if (empty || item == null) {
          setText(null)
          setGraphic(null)
        } else {
          setGraphic(getCell(item))
          val deleteItem = new MenuItem("Delete")
          deleteItem.setOnAction(_ => onDelete(item))
          setContextMenu(new ContextMenu(deleteItem))
        }
      }
    }
  }

  private def getCell(repositoryCell: RepositoryCell): GridPane = {
    val gridPane = new GridPane
    val label    = new Label(gitHelper.getRepoName(repositoryCell.git))

    val column1 = new ColumnConstraints
    column1.setHgrow(Priority.ALWAYS)
    column1.setFillWidth(true)
    val column2 = new ColumnConstraints
    column2.setHgrow(Priority.NEVER)

    column2.setHalignment(HPos.RIGHT)

    gridPane.getColumnConstraints.addAll(column1, column2)

    gridPane.add(label, 0, 0)
    gridPane.add(repositoryCell.progressMonitor.progressIndicator, 1, 0)

    gridPane
  }
}
