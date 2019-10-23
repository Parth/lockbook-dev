package lockbook.dev

import javafx.application.Platform
import javafx.geometry.HPos
import javafx.scene.control._
import javafx.scene.layout.{ColumnConstraints, GridPane, Priority}
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class RepositoryCell(git: Git, statusLabel: Label)
object RepositoryCell {
  def calculateStatus(repocell: RepositoryCell, gitHelper: GitHelper): Unit = Future {
    Future {
      val pullNeeded = gitHelper.pullNeeded(repocell.git).getOrElse(false) // TODO network?
      val localDirty = gitHelper.localDirty(repocell.git)

      val status = if (pullNeeded && localDirty) {
        "Sync"
      } else if (pullNeeded) {
        "Pull"
      } else if (localDirty) {
        "Push"
      } else {
        ""
      }

      Platform.runLater(() => repocell.statusLabel.setText(status))
    }
  }
}

class RepositoryCellUi(gitHelper: GitHelper) {

  def getListCell(
      onClick: Git => Unit,
      onDelete: RepositoryCell => Unit,
      onClone: () => Unit
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
          val newRepo    = new MenuItem("Clone Repository")
          val push       = new MenuItem("Push")
          val pull       = new MenuItem("Pull")
          val sync       = new MenuItem("Sync")

          deleteItem.setOnAction(_ => onDelete(item))

          newRepo.setOnAction(_ => onClone())
          push.setOnAction(_ => {
            gitHelper.commitAndPush("", item.git)
            RepositoryCell.calculateStatus(item, gitHelper)
          }) // Default push good settings candidate

          pull.setOnAction(_ => {
            gitHelper.pull(item.git)
            RepositoryCell.calculateStatus(item, gitHelper)
          })
          sync.setOnAction(_ => {
            gitHelper.sync(item.git)
            RepositoryCell.calculateStatus(item, gitHelper)
          })

          setContextMenu(new ContextMenu(newRepo, pull, push, sync, deleteItem))
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
    gridPane.add(repositoryCell.statusLabel, 1, 0)

    gridPane
  }
}
