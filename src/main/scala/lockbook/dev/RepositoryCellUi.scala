package lockbook.dev

import javafx.application.Platform
import javafx.geometry.HPos
import javafx.scene.Cursor
import javafx.scene.control._
import javafx.scene.layout.{ColumnConstraints, GridPane, Priority}
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RepositoryCell(git: Git, statusLabel: Label)
object RepositoryCell {
  def calculateStatus(repocell: RepositoryCell, gitHelper: GitHelper): Unit =
    Future {
      val maybePullNeeded = gitHelper.pullNeeded(repocell.git)
      val pullNeeded      = maybePullNeeded.getOrElse(false)
      val localDirty      = gitHelper.localDirty(repocell.git)

      val status =
        if (maybePullNeeded.isLeft) "Error"
        else if (pullNeeded && localDirty) "Both"
        else if (pullNeeded) "Pull"
        else if (localDirty) "Push"
        else ""

      Platform.runLater(() => repocell.statusLabel.setText(status))
    }
}

class RepositoryCellUi(gitHelper: GitHelper) {

  // These particular functions need to be passed in becaues they require both both the list element & list
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
          val deleteItem     = new MenuItem("Delete")
          val newRepo        = new MenuItem("Clone Repository")
          val push           = new MenuItem("Push")
          val pull           = new MenuItem("Pull")
          val commitPullPush = new MenuItem("Commit, Pull & Push")

          deleteItem.setOnAction(_ => onDelete(item))
          newRepo.setOnAction(_ => onClone())
          push.setOnAction(_ => pushClicked(item))
          pull.setOnAction(_ => pullClicked(item))

          commitPullPush.setOnAction(_ => commitPullPushClicked(item))

          setContextMenu(new ContextMenu(newRepo, pull, push, commitPullPush, deleteItem))
        }
      }
    }
  }

  private def commitPullPushClicked(item: RepositoryCell): Unit = {
    item.statusLabel.getScene.setCursor(Cursor.WAIT)

    Future {
      gitHelper.sync(item.git) match {
        case Left(error) =>
          Platform.runLater(() => AlertUi.showBad("Sync Failed!", error.uiMessage)) // TODO add repo names to these messages
        case Right(_) =>
      }

      Platform.runLater(() => {
        item.statusLabel.getScene.setCursor(Cursor.DEFAULT)
      })
    }
  }

  private def pushClicked(item: RepositoryCell): Unit = {
    item.statusLabel.getScene.setCursor(Cursor.WAIT)
    Future {
      gitHelper.commitAndPush("", item.git) match { // good settings candidate
        case Left(error) => Platform.runLater(() => AlertUi.showBad("Push Failed!", error.uiMessage))
        case Right(_)    =>
      }

      Platform.runLater(() => {
        item.statusLabel.getScene.setCursor(Cursor.DEFAULT)
      })
    }
  }

  private def pullClicked(item: RepositoryCell): Unit = {
    item.statusLabel.getScene.setCursor(Cursor.WAIT)
    Future {
      gitHelper.pull(item.git) match {
        case Left(error) => Platform.runLater(() => AlertUi.showBad("Pull Failed!", error.uiMessage))
        case Right(_)    =>
      }

      Platform.runLater(() => {
        item.statusLabel.getScene.setCursor(Cursor.DEFAULT)
      })
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
