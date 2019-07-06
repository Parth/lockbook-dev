package devbook

import rx.lang.scala.Observable

object PasswordStream {
  def apply(
      observable: Observable[PasswordAttempt]
  ): Observable[Either[PasswordSuccess, PasswordFailure]] =
    observable.map(Encryption.testPassword)
}
