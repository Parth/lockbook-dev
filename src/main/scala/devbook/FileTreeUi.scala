package devbook

import java.io.File

import javafx.scene.control.{TreeCell, TreeItem, TreeView}
import org.eclipse.jgit.api.Git

class FileTreeUi {
  def getView(git: Git, onSelected: File => Unit): TreeView[File] = {
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
            onSelected(newValue.getValue)
      )

    treeView
  }

  def getViewHelper(file: File): TreeItem[File] = {
    val item   = new TreeItem[File](file)
    val childs = file.listFiles
    if (childs != null) {
      for (child <- childs) {
        item.getChildren.add(getViewHelper(child))
      }
    }
    item
  }
}
