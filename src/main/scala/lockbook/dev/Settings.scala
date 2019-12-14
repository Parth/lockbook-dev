package lockbook.dev

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

case class LockbookSettings(
    theme: Option[Theme],
    autoLockTime: Option[AutoLockTime] // change variable name to autoLock
)

sealed trait Settings

trait Theme extends Settings { val fileName: String }
case object Light extends Theme {
  override val fileName: String = "light.css"
}

case class AutoLockTime(time : Option[FiniteDuration]) extends Settings // remove default value, implement somewhere else
