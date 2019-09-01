package lockbook.dev

case class PasswordAttempt(attempt: String)
case class Password(password: String)

trait PasswordHelper {
  var password: Password
  def testAndSetPassword(pwa: PasswordAttempt): Either[LockbookError, Password]
  def passwordIfMatch(password1: String, password2: String): Either[PasswordsDontMatch, Password]
  def setPassword(password: Password): Password
  def clearPassword(): Unit
}

class PasswordHelperImpl(lockfile: LockfileHelper, encryptionHelper: EncryptionHelper) extends PasswordHelper {

  var password: Password = _

  override def testAndSetPassword(pwa: PasswordAttempt): Either[LockbookError, Password] = {
    lockfile.getLockfile
      .flatMap(encrypted => encryptionHelper.decrypt(encrypted, Password(pwa.attempt)))
      .map(_ => Password(pwa.attempt))
      .map(setPassword)
  }

  override def setPassword(password: Password): Password = {
    this.password = password
    password
  }

  override def passwordIfMatch(p1: String, p2: String): Either[PasswordsDontMatch, Password] = {
    if (p1 == p2) {
      Right(Password(p1))
    } else {
      Left(PasswordsDontMatch())
    }
  }

  override def clearPassword(): Unit = this.password = null // Should make password optional?
}
