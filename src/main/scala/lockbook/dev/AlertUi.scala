package lockbook.dev

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}

object AlertUi {

  def showGood(title: String, message: String): Unit = {
    val alert = new Alert(AlertType.CONFIRMATION)
    alert.getDialogPane.getStylesheets.add("dark.css")
    alert.setTitle(title)
    alert.setGraphic(null)
    alert.setHeaderText(message)
    alert.getButtonTypes.remove(ButtonType.CANCEL)
    alert.show()
  }

  def showBad(title: String, message: String): Unit = {
    val alert = new Alert(AlertType.ERROR)
    alert.getDialogPane.getStylesheets.add("dark.css")
    alert.setTitle(title)
    alert.setGraphic(null)
    alert.setHeaderText(message)
    alert.getButtonTypes.remove(ButtonType.CANCEL)
    alert.show()

  }

}
