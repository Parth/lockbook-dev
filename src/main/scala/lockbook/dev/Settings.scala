package lockbook.dev

import scala.concurrent.duration.FiniteDuration

case class LockbookSettings(
    theme: Option[Theme],
    autoLockTime: Option[AutoLock] // change variable name to autoLock
)

sealed trait Settings

trait Theme extends Settings {
  val fileName: String
  val themeName: String
}

case object Light extends Theme {
  override val fileName: String = "light.css"
  override val themeName: String = "Light"
}

case class AutoLock(time: Option[FiniteDuration]) extends Settings // remove default value, implement somewhere else
