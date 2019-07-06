package devbook

import javafx.scene.Scene
import javafx.scene.control.SplitPane
import javafx.stage.Stage
import rx.lang.scala.Subject

class UI(stage: Stage) {

  private val root: SplitPane = new SplitPane
  private val uiStream: Subject[UIEvents] = Subject()

  def setup(): Unit = {
    stage.setTitle("Devbook")

    uiStream.subscribe(value => {
      value match {
        case _: OnStart =>
          Lockfile.getLockfile match {
            case Some(bytes) => uiStream.onNext(ShowPassword(bytes))
            case None        => uiStream.onNext(ShowNewDevbook())
          }

        case showPasswordEvent: ShowPassword =>
          val passwordUI = new PasswordUI(uiStream, showPasswordEvent.lockfile)
          root.getItems.add(passwordUI.getView)

        case _: ShowNewDevbook =>
          val newDevbookUI = new NewDevbookUI(uiStream)
          root.getItems.add(newDevbookUI.getView)

        case _ =>
      }
    })

    uiStream.subscribe(println(_))

    uiStream.onNext(OnStart())

    stage.setScene(new Scene(root, 300, 600))
    stage.show()
  }

}
object UI {
  def apply(stage: Stage): UI = new UI(stage)
}
