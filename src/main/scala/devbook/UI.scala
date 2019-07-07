package devbook

import javafx.scene.Scene
import javafx.scene.control.SplitPane
import javafx.stage.Stage
import rx.lang.scala.Subject

class UI(stage: Stage, primaryStream: Subject[Events]) {

  private val root: SplitPane = new SplitPane

  val passwordUI = new PasswordUI(primaryStream)
  val newDevbookUI = new NewDevbookUI(primaryStream)
  val newRepositoryUI = new RepositoryUI(primaryStream)

  def setup(): Unit = {
    stage.setTitle("Devbook")

    passwordUI.setupListeners()
    newDevbookUI.setupListeners()
    newRepositoryUI.setupListeners()

    primaryStream
      .collect { case events: UIEvents => events }
      .subscribe(value => {
        value match {
          case _: OnStart =>
            Lockfile.getLockfile match {
              case Some(bytes) => primaryStream.onNext(ShowPassword(bytes))
              case None        => primaryStream.onNext(ShowNewDevbook())
            }

          case _: ShowPassword =>
            root.getItems.add(passwordUI.getView)

          case _: ShowNewDevbook =>
            root.getItems.add(newDevbookUI.getView)

          case _: ShowRepository =>
            root.getItems.removeAll(root.getItems)
            root.getItems.add(newRepositoryUI.getView)
        }
      })

    primaryStream.subscribe(println(_))

    primaryStream.onNext(OnStart())
    stage.setScene(new Scene(root, 300, 600))
    stage.show()
  }

}
object UI {
  def apply(stage: Stage): UI = new UI(stage, Subject[Events]())
}
