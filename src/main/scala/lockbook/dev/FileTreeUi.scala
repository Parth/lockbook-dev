package lockbook.dev

import java.io.File

import javafx.scene.control._
import javafx.scene.layout.BorderPane
import javafx.stage.FileChooser
import org.eclipse.jgit.api.Git

class FileTreeUi(fileHelper: FileHelper) {

  def getView(git: Git, onSelected: (Git, File) => Unit): BorderPane = {
    // Setup TreeView
    val treeView = new TreeView[File]
    treeView.setRoot(getViewHelper(git.getRepository.getWorkTree))
    treeView.setShowRoot(false)
    treeView.setCellFactory(_ => fileToTreeCell)
    treeView.getSelectionModel.selectedItemProperty
      .addListener(
        (_, oldValue, newValue) =>
          if (newValue.isLeaf && oldValue != newValue)
            onSelected(git, newValue.getValue)
      )

    val root = new BorderPane

    val newFileButton = new Button("New")
    newFileButton.setOnAction(_ => {
      val fileChooser = new FileChooser
      fileChooser.setInitialDirectory(git.getRepository.getWorkTree)

      val file = fileChooser.showSaveDialog(root.getScene.getWindow)
      if (file != null) {
        file.createNewFile() // TODO, this returns a boolean
        onSelected(git, file) // TODO insert this into the right place
        treeView.setRoot(getViewHelper(git.getRepository.getWorkTree))
      }
    })

    root.setCenter(treeView)
    root.setBottom(newFileButton)
    root
  }

  def getViewHelper(file: File): TreeItem[File] = {
    val item: TreeItem[File]            = new TreeItem[File](file)
    val childrenUnfiltered: Array[File] = file.listFiles

    if (childrenUnfiltered != null) {
      val children = childrenUnfiltered.filter(_.getName != ".git").sortBy(_.getName)
      for (child <- children) {
        item.getChildren.add(getViewHelper(child))
      }
    }
    item
  }

  private def fileToTreeCell: TreeCell[File] = new TreeCell[File]() {
    override def updateItem(item: File, empty: Boolean): Unit = {
      super.updateItem(item, empty)
      if (item != null) {

        // Right Click to Delete
        val contextMenu = new ContextMenu
        val menuItem    = new MenuItem("Delete")
        contextMenu.getItems.add(menuItem)
        menuItem.setOnAction(_ => {
          fileHelper.recursiveFileDelete(item)
          val node = getTreeItem
          node.getParent.getChildren.remove(node)
        })
        setContextMenu(contextMenu)

        setText(item.getName)
      } else {
        setText("")
      }
    }
  }
}
