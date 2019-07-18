package devbook

import java.io.File

import javafx.scene.control.{Button, TreeCell, TreeItem, TreeView}
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import org.eclipse.jgit.api.Git

class FileTreeUi {

  def getView(git: Git, onSelected: (Git, File) => Unit): BorderPane = {
    val treeView = new TreeView[File]
    treeView.setRoot(getViewHelper(git.getRepository.getWorkTree))
    treeView.setCellFactory(
      _ =>
        new TreeCell[File]() {
          override def updateItem(item: File, empty: Boolean): Unit = {
            super.updateItem(item, empty)
            if (item != null) {
              setText(item.getName)
            } else {
              setText("")
            }
          }
        }
    )

    treeView.getSelectionModel.selectedItemProperty
      .addListener(
        (_, _, newValue) =>
          if (newValue.isLeaf)
            onSelected(git, newValue.getValue)
      )

    val root = new BorderPane
    root.setCenter(treeView)

    val newFileButton = new Button("New")
    newFileButton.setOnAction(_ => {
      val fileChooser = new FileChooser
      fileChooser.setInitialDirectory(git.getRepository.getWorkTree)

      val file = fileChooser.showSaveDialog(root.getScene.getWindow)
      if (file != null) {
        file.createNewFile() // TODO, this returns a boolean
        onSelected(git, file)
        treeView.setRoot(getViewHelper(git.getRepository.getWorkTree))
      }
    })

    root.setBottom(newFileButton)

    root
  }

  def getViewHelper(file: File): TreeItem[File] = {
    val item: TreeItem[File]  = new TreeItem[File](file)
    val children: Array[File] = file.listFiles

    if (children != null) {
      for (child <- children) {
        item.getChildren.add(getViewHelper(child))
      }
    }
    item
  }
}
