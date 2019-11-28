package lockbook.dev

import java.util.Optional

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Alert.AlertType
import javafx.scene.control._
import javafx.scene.layout.GridPane

class SettingsUi(settingsHelper: SettingsHelper) {

  def getView(): Unit = { //use graphbox

    val applyButton  = new ButtonType("Apply", ButtonBar.ButtonData.APPLY)
    val cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
    val alert        = new Alert(AlertType.CONFIRMATION, null, applyButton, cancelButton)

    val gridPane = new GridPane

    getViewHelper(alert, gridPane)

    val result: Optional[ButtonType] = alert.showAndWait()
    if (result.get() == applyButton) setSettings(alert.getDialogPane.getContent) else println(result.get())
  }

  def getViewHelper(alert: Alert, gridPane: GridPane): Unit = {
    val stylesBox       = new ComboBox[String](FXCollections.observableArrayList(Light.fileName))
    val autoLockTimeBox = new ComboBox[Int](FXCollections.observableArrayList(5))

    alert.setTitle("Settings")
    alert.setHeaderText("Settings")
    alert.setGraphic(null)

    gridPane.setHgap(10)
    gridPane.setVgap(10)
    gridPane.setPadding(new Insets(10))

    gridPane.add(new Label("Styles"), 0, 1)
    gridPane.add(stylesBox, 1, 1)
    gridPane.add(new Label("Auto Lock Time"), 0, 2)
    gridPane.add(autoLockTimeBox, 1, 2)

    alert.getDialogPane.getStylesheets.add("light.css")
    alert.getDialogPane.setContent(gridPane)
  }

  def setSettings(node: Node): Unit = {
    node.
  }

}
