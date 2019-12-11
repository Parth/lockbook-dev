package lockbook.dev

import scala.concurrent.duration.FiniteDuration

case class LockbookSettings(
    theme: Theme,
    autoLockTime: Option[AutoLockTime]
)

sealed trait Settings // rename to settings and call file lockbooksettings (move case class)

trait Theme extends Settings { val fileName: String }
case object Light extends Theme {
  override val fileName: String = "light.css"
}

case class AutoLockTime(time: Option[FiniteDuration]) extends Settings
