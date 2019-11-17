package lockbook.dev

sealed trait DefaultSettings

trait Theme extends DefaultSettings { val fileName: String}
case object Light extends Theme {
  override val fileName: String = "light.css"
}

trait AutoLockTime extends DefaultSettings { val time: Int}
case object ShortLockTime extends AutoLockTime {
  override val time: Int = 20
}
