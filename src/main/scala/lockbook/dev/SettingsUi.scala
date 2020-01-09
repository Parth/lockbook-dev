package lockbook.dev

import java.util.Optional
import java.util.concurrent.TimeUnit

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control
import javafx.scene.control.{TextFormatter, _}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.GridPane

import scala.concurrent.duration.FiniteDuration

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

      val restartDialog: Dialog[ButtonType] = new Dialog[ButtonType]()
      restartDialog.getDialogPane.getButtonTypes.addAll(ButtonType.OK)
      restartDialog.getDialogPane.setContentText("Restart Lockbook for settings to take effect.")
      restartDialog.getDialogPane.getStylesheets.add(settingsHelper.getTheme.fileName)
      restartDialog.showAndWait()
    }
  }

  private def showViewHelper(dialog: Dialog[LockbookSettings], gridPane: GridPane): Unit = {
    val stylesBox        = new ComboBox[Theme](FXCollections.observableArrayList(Light))
    val autoLockIntField = new TextField()
    val autoLockCheckBox = new control.CheckBox()
    val durationLabel    = new Label("Duration:") // included to use id to remove node

    stylesBox.getSelectionModel.select(settingsHelper.getTheme) // TODO: Make autolockIntField appear after lockbookCheckBox is unchecked, intuitive

    if (settingsHelper.getAutoLock.time.isEmpty) autoLockCheckBox.setSelected(false)
    else {
      autoLockCheckBox.setSelected(true)
      autoLockIntField.setText(settingsHelper.getAutoLock.time.get.toMinutes.toString)
    }

    autoLockCheckBox.setOnAction(
      _ => {
        if (autoLockCheckBox.isSelected) {
          gridPane.add(durationLabel, 0, 4)
          gridPane.add(autoLockIntField, 1, 4)
        }

        if (!autoLockCheckBox.isSelected) {
          gridPane.getChildren.remove(durationLabel)
          gridPane.getChildren.remove(autoLockIntField)
        }
      }
    )

    autoLockIntField.setTextFormatter(new TextFormatter[String]((change: TextFormatter.Change) => {
      change.setText(change.getText.replaceAll("""\D+""", ""))

      if (!(change.getText.length == 0 || autoLockIntField.getText.length == 0))
        if ((autoLockIntField.getText + change.getText).toShortOption.isEmpty || autoLockIntField.getText.length == 0 && change.getText == "0") // use short limit to avoid specific problems from finiteduration preference
          change.setText("")

      change
    }))

    dialog.setTitle("Settings") // use settingshelper across, then advanced capabil.
    dialog.setHeaderText("Settings")
    dialog.setGraphic(null)

    gridPane.setHgap(10)
    gridPane.setVgap(10)
    gridPane.setPadding(new Insets(10))
    gridPane.setMinSize(300, 200)

    gridPane.add(new Label("Styles"), 0, 1)
    gridPane.add(stylesBox, 1, 1)
    gridPane.add(new Label("Auto Lock Time"), 0, 2)
    gridPane.add(new Label("Enable:"), 0, 3)
    gridPane.add(autoLockCheckBox, 1, 3)

    if (autoLockCheckBox.isSelected) {
      gridPane.add(durationLabel, 0, 4)
      gridPane.add(autoLockIntField, 1, 4)
    }

    dialog.getDialogPane.getStylesheets.add(settingsHelper.getTheme.fileName)
    dialog.getDialogPane.setContent(gridPane)


    dialog.setResultConverter(
      dialogButton => {
        if (dialogButton == ButtonType.APPLY)
          LockbookSettings(
            Some(stylesBox.getValue),
            Some( // can simplify
              if (!autoLockCheckBox.isSelected) AutoLock(None)
              else AutoLock(Some(FiniteDuration(autoLockIntField.getText.toLong, TimeUnit.MINUTES)))
            )
          )
        else null
      }
    )
  }
}
