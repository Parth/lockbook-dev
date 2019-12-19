package lockbook.dev

import java.util.Optional

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control._
import javafx.scene.layout.GridPane

class SettingsUi(settingsHelper: SettingsHelper, fileHelper: FileHelper) {

  def showView(): Unit = {

    val dialog: Dialog[LockbookSettings] = new Dialog[LockbookSettings]()

    dialog.getDialogPane.getButtonTypes.addAll(
      ButtonType.APPLY,
      ButtonType.CANCEL
    )

    val gridPane = new GridPane
    showViewHelper(dialog, gridPane)

    val result: Optional[LockbookSettings] = dialog.showAndWait()
    if (result.isPresent) {
      SettingsHelper.constructJson(result.get(), fileHelper)
    }
  }

  private def showViewHelper(dialog: Dialog[LockbookSettings], gridPane: GridPane): Unit = {
    val stylesBox       = new ComboBox[Theme](FXCollections.observableArrayList(Light)) // TODO change type to theme
    val autoLockTimeBox = new TextField()

    stylesBox.getSelectionModel.select(settingsHelper.getTheme) // can shorten this for future settings; make loop
    autoLockTimeBox.setText(settingsHelper.getAutoLock.asInstanceOf[String])

    autoLockTimeBox.setOnKeyReleased(
      _ =>
        if (!autoLockTimeBox.getText.matches("""\d*""")) {
          autoLockTimeBox.setText(autoLockTimeBox.getText.replaceAll("""\D+""", ""))
        }
    )

    dialog.setTitle("Settings") // use settingshelper across, then advanced capabil.
    dialog.setHeaderText("Settings")
    dialog.setGraphic(null)

    gridPane.setHgap(10)
    gridPane.setVgap(10)
    gridPane.setPadding(new Insets(10))

    gridPane.add(new Label("Styles"), 0, 1)
    gridPane.add(stylesBox, 1, 1)
    gridPane.add(new Label("Auto Lock Time"), 0, 2)
    gridPane.add(autoLockTimeBox, 1, 2)

    dialog.getDialogPane.getStylesheets.add(settingsHelper.getTheme.fileName)
    dialog.getDialogPane.setContent(gridPane)

    dialog.setResultConverter(
      dialogButton => {
        if (dialogButton == ButtonType.APPLY)
          LockbookSettings(
            Some(stylesBox.getValue.asInstanceOf[Theme]),
            Some(autoLockTimeBox.getText.asInstanceOf[AutoLock])
          )
        else null
      }
    )
  }

}
