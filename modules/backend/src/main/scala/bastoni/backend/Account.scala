package bastoni.backend

import bastoni.domain.model.{User, UserId}
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.all.*
import io.circe.parser.decode
import org.http4s.server.AuthMiddleware
import org.typelevel.ci.CIString

case class Account(user: User)

object Account:
  def insecureMiddleware: AuthMiddleware[IO, Account] = AuthMiddleware(
    Kleisli(request =>
      for
        auth <- OptionT.fromOption(request.cookies.find(_.name == "auth"))
        user <- OptionT.fromOption(decode[User](auth.content).toOption)
      yield Account(user)
    )
  )
