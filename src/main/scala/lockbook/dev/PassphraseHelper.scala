package lockbook.dev

case class PassphraseAttempt(attempt: String)
case class Passphrase(passphrase: String)

trait PassphraseHelper {
  var passphrase: Passphrase
  def testAndSetPassphrase(pwa: PassphraseAttempt): Either[LockbookError, Passphrase]
  def passphraseIfMatch(passphrase1: String, passphrase2: String): Either[PassphrasesDoNotMatch, Passphrase]
  def setPassphrase(passphrase: Passphrase): Passphrase
  def clearPassphrase(): Unit
}

class PassphraseHelperImpl(lockfile: LockfileHelper, encryptionHelper: EncryptionHelper) extends PassphraseHelper {

  var passphrase: Passphrase = _

  override def testAndSetPassphrase(pwa: PassphraseAttempt): Either[LockbookError, Passphrase] = {
    lockfile.getLockfile
      .flatMap(encrypted => encryptionHelper.decrypt(encrypted, Passphrase(pwa.attempt)))
      .map(_ => Passphrase(pwa.attempt))
      .map(setPassphrase)
  }

  override def setPassphrase(passphrase: Passphrase): Passphrase = {
    this.passphrase = passphrase
    passphrase
  }

  override def passphraseIfMatch(p1: String, p2: String): Either[PassphrasesDoNotMatch, Passphrase] = {
    if (p1 == p2) {
      Right(Passphrase(p1))
    } else {
      Left(PassphrasesDoNotMatch())
    }
  }

  override def clearPassphrase(): Unit = this.passphrase = null // Should make passphrase optional?
}
