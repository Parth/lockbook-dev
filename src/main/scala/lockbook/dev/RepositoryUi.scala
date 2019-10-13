package lockbook.dev

import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.Pos
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

  val repoList: ObservableList[RepositoryCell] = FXCollections.observableArrayList[RepositoryCell]()

  def getView(onClick: Git => Unit): BorderPane = {
    val borderPane    = new BorderPane
    val newRepoButton = new Button("New Repository")
    val listView      = new ListView[RepositoryCell]

    borderPane.setId("repoList")

    Future {
      repoList.removeAll(repoList)
      gitHelper.getRepositories
        .map(RepositoryCell.fromGit(_, gitHelper))
        .map(repoList.add)

      pullAllRepos()
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

    BorderPane.setAlignment(newRepoButton, Pos.CENTER)
    borderPane.setCenter(listView)
    borderPane.setBottom(newRepoButton)
    borderPane
  }

  def delete(list: ListView[RepositoryCell])(repositoryCell: RepositoryCell): Unit = {
    gitHelper.deleteRepo(repositoryCell.git)
    list.getItems.remove(repositoryCell)
  }

  def pullAllRepos(): Unit = Future {
    println("pull all repos")
    repoList
      .stream()
      .forEach(repoCell => println(gitHelper.pull(repoCell.git, repoCell.pullCommand)))
  }
}
