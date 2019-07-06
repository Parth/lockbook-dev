package devbook

sealed trait UIEvents

case class OnStart() extends UIEvents

case class ShowNewDevbook() extends UIEvents

case class ShowPassword(lockfile: EncryptedValue) extends UIEvents

// TODO these should not subclass UIEvents, so that UI.scala can do complete case matching
case class PasswordAttempt(attempt: String, encryptedValue: EncryptedValue)
    extends UIEvents
case class PasswordSuccess(password: String) extends UIEvents
case class PasswordFailure(message: String) extends UIEvents

case class ShowRepository() extends UIEvents

case class PopupError(message: String) extends UIEvents
