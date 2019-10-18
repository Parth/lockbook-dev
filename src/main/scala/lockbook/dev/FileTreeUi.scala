package lockbook.dev

import java.io.File
import java.util.Optional

import javafx.scene.control.{TextInputDialog, _}
import javafx.scene.layout.BorderPane
import org.eclipse.jgit.api.Git

class FileTreeUi(fileHelper: FileHelper) {

  def getView(git: Git, onSelected: (Git, File) => Unit): BorderPane = {
    // Setup TreeView
    val treeView = new TreeView[File]
    treeView.setRoot(getViewHelper(git.getRepository.getWorkTree))
    treeView.setShowRoot(false)
    treeView.setCellFactory(_ => fileToTreeCell(git, onSelected))
    treeView.getSelectionModel.selectedItemProperty
      .addListener(
        (_, oldValue, newValue) =>
          if (newValue.isLeaf && oldValue != newValue && newValue.getValue.isFile)
            onSelected(git, newValue.getValue)
      )

    val root = new BorderPane

    root.setCenter(treeView)
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

  private def fileToTreeCell(git: Git, onSelected: (Git, File) => Unit): TreeCell[File] =
    new TreeCell[File]() {
      override def updateItem(item: File, empty: Boolean): Unit = {
        super.updateItem(item, empty)
        if (item != null) {

          val contextMenu = new ContextMenu
          val delete      = new MenuItem("Delete")
          val newFile     = new MenuItem("New File")
          val newFolder   = new MenuItem("New Folder")
          contextMenu.getItems.addAll(newFolder, newFile, delete)

          val enclosingFolderNode = if (getTreeItem.getValue.isDirectory) {
            getTreeItem
          } else {
            getTreeItem.getParent
          }

          delete.setOnAction(_ => {
            fileHelper.recursiveFileDelete(item)
            getTreeItem.getParent.getChildren.remove(getTreeItem)
          })

          // TODO consolidate this code more
          newFile.setOnAction(_ => {
            val parentDirectory = if (!item.isDirectory) item.getParentFile else item
            newFileOrFolderDialogResult(true).map(name => s"${parentDirectory.getAbsolutePath}/$name") match {
              case Some(newFileName) =>
                val newFile = new File(newFileName)
                newFile.createNewFile()

                val newTreeItem = new TreeItem[File](newFile)
                enclosingFolderNode.getChildren.add(newTreeItem)

                val location = getTreeView.getRow(newTreeItem)
                getTreeView.getSelectionModel.select(location)

              case None =>
            }
          })

          newFolder.setOnAction(_ => {
            val parentDirectory = if (!item.isDirectory) item.getParentFile else item
            newFileOrFolderDialogResult(false).map(name => s"${parentDirectory.getAbsolutePath}/$name/") match {
              case Some(newFileName) =>
                val newFile = new File(newFileName)
                newFile.mkdirs()

                println(newFile.isDirectory)

                val newTreeItem = new TreeItem[File](newFile)
                enclosingFolderNode.getChildren.add(newTreeItem)

                val location = getTreeView.getRow(newTreeItem)
                getTreeView.getSelectionModel.select(location)

              case None =>
            }
          })

          setContextMenu(contextMenu)
          setText(item.getName)
        } else {
          setText("")
        }
      }
    }

  private def newFileOrFolderDialogResult(isFile: Boolean): Option[String] = {
    val fileOrFolder = if (isFile) "file" else "folder"

    val dialog = new TextInputDialog

    dialog.setTitle(s"Create new $fileOrFolder")
    dialog.setHeaderText(s"Enter $fileOrFolder name")
    dialog.setContentText("Name:")

    val result: Optional[String] = dialog.showAndWait


    if (result.isPresent) Some(result.get()) else None
  }

}
