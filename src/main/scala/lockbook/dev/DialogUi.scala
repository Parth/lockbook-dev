package lockbook.dev

import java.util.Optional

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType, TextInputDialog}

class DialogUi(settingsHelper: SettingsHelper) {
  def showGood(title: String, message: String): Unit = {
    val alert = new Alert(AlertType.CONFIRMATION)
    alert.getDialogPane.getStylesheets.add(settingsHelper.getTheme.fileName)
    alert.setTitle(title)
    alert.setGraphic(null)
    alert.setHeaderText(message)
    alert.getButtonTypes.remove(ButtonType.CANCEL)
    alert.show()
  }

  def showBad(title: String, message: String): Unit = {
    val alert = new Alert(AlertType.ERROR)
    alert.getDialogPane.getStylesheets.add(settingsHelper.getTheme.fileName)
    alert.setTitle(title)
    alert.setGraphic(null)
    alert.setHeaderText(message)
    alert.getButtonTypes.remove(ButtonType.CANCEL)
    alert.show()
  }

  def askUserForString(title: String, header: String, content: String): Option[String] = {
    val dialog = new TextInputDialog

    dialog.getDialogPane.getStylesheets.add(settingsHelper.getTheme.fileName)
    dialog.setTitle(title)
    dialog.setHeaderText(header)
    dialog.setContentText(content)

    val result: Optional[String] = dialog.showAndWait

    if (result.isPresent) Some(result.get()) else None
  }
}
