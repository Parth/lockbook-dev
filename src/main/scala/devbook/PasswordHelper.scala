package devbook

case class PasswordAttempt(attempt: String)
case class Password(password: String)

trait PasswordHelper {
  var password: Option[Password]
  def testPassword(pwa: PasswordAttempt): Either[Password, Error]
}

class PasswordHelperImpl(lockfile: LockfileHelper, encryptionHelper: EncryptionHelper)
    extends PasswordHelper {

  var password: Option[Password] = None

  override def testPassword(pwa: PasswordAttempt): Either[Password, Error] = {
    lockfile.getLockfile match {
      case Some(value) =>
        encryptionHelper.decrypt(value, Password(pwa.attempt)) match {
          case Left(_) =>
            val successPassword = Password(pwa.attempt)
            password = Some(successPassword)
            Left(successPassword)
          case Right(error) => Right(error)
        }

      case None => // highly unlikely if this is being called
        Right(new Error("Lockfile does not exist, please restart the application"))
    }
  }
}
