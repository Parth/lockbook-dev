package lockbook.dev

import java.util.Optional

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.layout.GridPane

import scala.concurrent.duration.FiniteDuration

class SettingsUi(settingsHelper: SettingsHelper, fileHelper: FileHelper) {

  def addView(): Unit = {

    val dialog: Dialog[Settings] = new Dialog[Settings]()

    dialog.getDialogPane.getButtonTypes.addAll(
      ButtonType.APPLY,
      ButtonType.CANCEL
    )

    val gridPane = new GridPane
    addViewHelper(dialog, gridPane)

    val result: Optional[Settings] = dialog.showAndWait()
    if (result.isPresent) {
      SettingsHelper.constructJson(result.get(), fileHelper)
    }
  }

  def addViewHelper(dialog: Dialog[Settings], gridPane: GridPane): Unit = {
    val stylesBox       = new ComboBox[String](FXCollections.observableArrayList(Light.fileName))
    val autoLockTimeBox = new ComboBox[Int](FXCollections.observableArrayList(30, 60, 90, 120))

    stylesBox.getSelectionModel.select(settingsHelper.getTheme) // can shorten this for future settings; make loop
    autoLockTimeBox.getSelectionModel.select(settingsHelper.getAutoLockTime)

    dialog.setTitle("Settings")
    dialog.setHeaderText("Settings")
    dialog.setGraphic(null)

    gridPane.setHgap(10)
    gridPane.setVgap(10)
    gridPane.setPadding(new Insets(10))

    gridPane.add(new Label("Styles"), 0, 1)
    gridPane.add(stylesBox, 1, 1)
    gridPane.add(new Label("Auto Lock Time"), 0, 2)
    gridPane.add(autoLockTimeBox, 1, 2)

    dialog.getDialogPane.getStylesheets.add("light.css")
    dialog.getDialogPane.setContent(gridPane)


    dialog.setResultConverter(
      dialogButton => {
        if (dialogButton == ButtonType.APPLY)
          new Settings(Some(stylesBox.getValue), Some(autoLockTimeBox.getValue))
        else null
      }
    )
  }

}
