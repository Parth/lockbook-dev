package lockbook.dev

import scala.util.{Failure, Success, Try}

case class PasswordAttempt(attempt: String)
case class Password(password: String)

trait PasswordHelper {
  var password: Password
  def testPassword(pwa: PasswordAttempt): Try[Password]
  def doMatch(password1: String, password2: String): Try[Unit]
  def setPassword(password: Password): Password
}

class PasswordHelperImpl(lockfile: LockfileHelper, encryptionHelper: EncryptionHelper)
    extends PasswordHelper {

  var password: Password = _

  override def testPassword(pwa: PasswordAttempt): Try[Password] = {
    lockfile.getLockfile
      .flatMap(encryptionHelper.decrypt(_, Password(pwa.attempt)))
      .map(_ => Password(pwa.attempt))
      .map(setPassword)
  }

  override def setPassword(password: Password): Password = {
    this.password = password
    password
  }

  override def doMatch(password1: String, password2: String): Try[Unit] = {
    if (password1 == password2) {
      Success()
    } else {
      Failure(new Error("Passwords do not match"))
    }
  }
}
