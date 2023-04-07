package bastoni.backend

import bastoni.domain.model.{User, UserId}
import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.all.*
import org.http4s.server.AuthMiddleware
import org.typelevel.ci.CIString

case class Account(user: User)

object Account:
  def middleware: AuthMiddleware[IO, Account] = AuthMiddleware(
    Kleisli(request =>
      for
        names <- OptionT.fromOption(request.headers.get(CIString("x-user-name")))
        ids   <- OptionT.fromOption(request.headers.get(CIString("x-user-id")))
        id    <- OptionT.fromOption(UserId.tryParse(ids.head.value))
      yield Account(User(id, names.head.value))
    )
  )
