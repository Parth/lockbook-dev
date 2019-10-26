package lockbook.dev

import java.io.ByteArrayOutputStream
import java.security.{NoSuchAlgorithmException, SecureRandom}
import java.util
import java.util.Base64

import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKeyFactory}

case class EncryptedValue(garbage: String)
case class DecryptedValue(secret: String)

trait EncryptionHelper {
  def encrypt(value: DecryptedValue, passphrase: Passphrase): Either[CryptoError, EncryptedValue]
  def decrypt(garbage: EncryptedValue, passphrase: Passphrase): Either[CryptoError, DecryptedValue]
}

class EncryptionImpl extends EncryptionHelper {
  def encrypt(value: DecryptedValue, passphrase: Passphrase): Either[CryptoError, EncryptedValue] =
    try {
      Right(EncryptedValue(encryptHelper(value.secret, passphrase.passphrase)))
    } catch {
      case a: NoSuchAlgorithmException => Left(SecureOperationsNotSupported(a))
    }

  def decrypt(garbage: EncryptedValue, passphrase: Passphrase): Either[CryptoError, DecryptedValue] =
    try {
      if (garbage.garbage.isEmpty)
        Right(DecryptedValue(""))
      else
        Right(DecryptedValue(decryptHelper(garbage.garbage, passphrase.passphrase)))
    } catch {
      case _: IllegalArgumentException => Left(NotBase64())
      case a: NoSuchAlgorithmException => Left(SecureOperationsNotSupported(a))
      case _: Throwable                => Left(WrongPassphrase())
    }

  private def encryptHelper(str: String, passphrase: String): String = {
    val random = new SecureRandom
    val salt   = new Array[Byte](16)
    random.nextBytes(salt)

    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val spec    = new PBEKeySpec(passphrase.toCharArray, salt, 65536, 256)
    val tmp     = factory.generateSecret(spec)
    val secret  = new SecretKeySpec(tmp.getEncoded, "AES")
    val cipher  = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secret)

    val params        = cipher.getParameters
    val iv            = params.getParameterSpec(classOf[IvParameterSpec]).getIV
    val encryptedText = cipher.doFinal(str.getBytes("UTF-8"))

    // concatenate salt + iv + ciphertext
    val outputStream = new ByteArrayOutputStream
    outputStream.write(salt)
    outputStream.write(iv)
    outputStream.write(encryptedText)

    // properly encode the complete ciphertext
    new String(Base64.getEncoder.encode(outputStream.toByteArray))
  }

  private def decryptHelper(str: String, passphrase: String): String = {
    val ciphertext = Base64.getDecoder.decode(str)
    if (ciphertext.length < 48) return null
    val salt    = util.Arrays.copyOfRange(ciphertext, 0, 16)
    val iv      = util.Arrays.copyOfRange(ciphertext, 16, 32)
    val ct      = util.Arrays.copyOfRange(ciphertext, 32, ciphertext.length)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val spec    = new PBEKeySpec(passphrase.toCharArray, salt, 65536, 256)
    val tmp     = factory.generateSecret(spec)
    val secret  = new SecretKeySpec(tmp.getEncoded, "AES")
    val cipher  = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv))
    val plaintext = cipher.doFinal(ct)
    new String(plaintext, "UTF-8")
  }
}
