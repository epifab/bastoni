package bastoni.backend

import bastoni.domain.model.{User, UserId}
import bastoni.domain.view.ConnectionError
import cats.effect.IO
import cats.syntax.all.*
import io.circe.parser.decode
import io.circe.syntax.EncoderOps

case class Account(user: User)

trait AuthController[F[_]]:
  def authenticate(token: String): F[Either[ConnectionError, Account]]
  def tokenize(user: Account): F[String]

object InsecureAuthController extends AuthController[IO]:
  override def authenticate(token: String): IO[Either[ConnectionError, Account]] =
    IO.pure(decode[User](token) match
      case Left(_)     => Left(ConnectionError.InvalidToken)
      case Right(user) => Right(Account(user))
    )

  override def tokenize(account: Account): IO[String] =
    IO.pure(account.user.asJson.noSpaces)

