package lockbook.dev

import javafx.collections.FXCollections
import javafx.scene.control._
import javafx.scene.layout._
import org.eclipse.jgit.api.Git

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RepositoryUi(
    gitHelper: GitHelper,
    repositoryCellUi: RepositoryCellUi,
    cloneRepoDialog: CloneRepoDialog
) {

  def getView(onClick: Git => Unit): BorderPane = {
    val borderPane    = new BorderPane
    val newRepoButton = new Button("New Repository")
    val footer        = new HBox(newRepoButton)
    val repoList      = FXCollections.observableArrayList[RepositoryCell]()
    val listView      = new ListView[RepositoryCell]

    Future {
      val allRepositories = gitHelper.getRepositories
        .map(RepositoryCell.fromGit(_, gitHelper))

      allRepositories
        .map(repoList.add)

      allRepositories.map(repoCell => gitHelper.pull(repoCell.git, repoCell.pullCommand))

    }
    listView
      .cellFactoryProperty()
      .setValue(_ => repositoryCellUi.getListCell(onClick, delete(listView)))

    listView.getSelectionModel
      .selectedItemProperty()
      .addListener((_, old, newVal) => {
        if (old != newVal) {
          onClick(newVal.git) // TODO: fails if new val is null, happens if you delete the last repository
        }
      })

    listView.setItems(repoList)

    newRepoButton.setOnAction(_ => {
      cloneRepoDialog.showDialog(repoList)
    })

    borderPane.setCenter(listView)
    borderPane.setBottom(footer)
    borderPane
  }

  def delete(list: ListView[RepositoryCell])(repositoryCell: RepositoryCell): Unit = {
    gitHelper.deleteRepo(repositoryCell.git)
    list.getItems.remove(repositoryCell)
  }
}
