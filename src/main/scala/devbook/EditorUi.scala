package devbook

import java.io.File

import javafx.scene.control.TextArea
import javafx.scene.layout.BorderPane

import scala.io.Source

class EditorUi {

  def getView(f: File): BorderPane = {
    val root     = new BorderPane
    val textArea = new TextArea
    val text     = Source.fromFile(f.getAbsoluteFile).getLines.mkString("\n")
    textArea.setText(text)
    root.setCenter(textArea)
    root
  }

}
