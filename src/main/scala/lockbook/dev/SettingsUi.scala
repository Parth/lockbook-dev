package lockbook.dev

import javafx.collections.{FXCollections, ObservableList}
import javafx.geometry.{Insets, Pos}
import javafx.scene.Scene
import javafx.scene.control.{ComboBox, Label}
import javafx.scene.layout.{BorderPane, VBox}
import javafx.stage.Stage

class SettingsUi(settingsHelper: SettingsHelper) {

  def getView: Unit = {
    val stage           = new Stage
    val vBox            = new VBox
    val label = new Label("Settings")
    val stylesBox       = new ComboBox[String](FXCollections.observableArrayList(Light.fileName))
    stylesBox.set
    val autoLockTimeBox = new ComboBox[Int](FXCollections.observableArrayList(ShortLockTime.time))

    vBox.getChildren.addAll(label, stylesBox, autoLockTimeBox)
    vBox.setAlignment(Pos.BASELINE_CENTER)
    vBox.setPadding(new Insets(20))
    vBox.setSpacing(10)

    val borderPane = new BorderPane(vBox)

    stage.setScene(new Scene(borderPane))
    stage.setTitle("Settings")
    stage.showAndWait()
  }
}
