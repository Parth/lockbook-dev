package lockbook.dev

import javafx.application.Platform
import javafx.scene.{Cursor, Scene}

import scala.concurrent.{ExecutionContext, Future}

object DoInBackgroundWithMouseSpinning {
  def apply(dialogUi: DialogUi, name: String, task: () => Either[LockbookError, Any], scene: Scene)(
      implicit ec: ExecutionContext
  ): Unit = {
    scene.setCursor(Cursor.WAIT)

    Future {
      task() match {
        case Left(error) =>
          Platform.runLater(() => dialogUi.showBad(s"$name Failed!", error.uiMessage))
        case Right(_) =>
      }

      Platform.runLater(() => {
        scene.setCursor(Cursor.DEFAULT)
      })
    }
  }

}
