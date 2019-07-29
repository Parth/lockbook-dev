package lockbook.dev

import javafx.collections.FXCollections
import javafx.scene.control._
import javafx.scene.layout._
import org.eclipse.jgit.api.Git

class RepositoryUi(
    gitHelper: GitHelper,
    repositoryCellUi: RepositoryCellUi,
    cloneRepoDialog: CloneRepoDialog
) {

  def getView(onClick: Git => Unit): BorderPane = {
    val borderPane    = new BorderPane
    val newRepoButton = new Button("New Repository")
    val footer        = new HBox(newRepoButton)
    val repoList      = FXCollections.observableArrayList[Git]()
    val listView      = new ListView[Git]

    gitHelper.getRepositories.foreach(repoList.add)

    listView.setItems(repoList)
    listView
      .cellFactoryProperty()
      .setValue(_ => repositoryCellUi.getListCell(onClick))

    newRepoButton.setOnAction(_ => {
      cloneRepoDialog.showDialog(repoList)
    })

    borderPane.setCenter(listView)
    borderPane.setBottom(footer)
    borderPane
  }
}
