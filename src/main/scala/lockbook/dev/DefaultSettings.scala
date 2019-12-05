package lockbook.dev

import scala.concurrent.duration.FiniteDuration

sealed trait DefaultSettings

trait Theme extends DefaultSettings { val fileName: String }
case object Light extends Theme {
  override val fileName: String = "light.css"
}

case class AutoLockTime(time: FiniteDuration) extends DefaultSettings