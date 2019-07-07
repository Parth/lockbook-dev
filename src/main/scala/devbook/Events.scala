package devbook

sealed trait Events

sealed trait UIEvents extends Events

case class OnStart() extends UIEvents
case class ShowNewDevbook() extends UIEvents
case class ShowPassword(lockfile: EncryptedValue) extends UIEvents
case class ShowRepository() extends UIEvents
case class PopupError(message: String) extends UIEvents

sealed trait PasswordEvents extends Events

case class PasswordAttempt(attempt: String, encryptedValue: EncryptedValue)
    extends PasswordEvents
case class PasswordSuccess(password: String) extends PasswordEvents
case class PasswordFailure(message: String) extends PasswordEvents
