package bastoni.backend.routes

import bastoni.backend.{Account, AuthController}
import bastoni.domain.model.{User, UserId}
import cats.effect.IO
import io.circe.syntax.EncoderOps
import io.circe.Json
import org.http4s.{HttpRoutes, ResponseCookie}
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.io.*
import org.typelevel.ci.CIString

object AuthControllerRoutes:
  def routes(authController: AuthController[IO]): HttpRoutes[IO] = HttpRoutes.of { case req @ POST -> Root =>
    req.headers.get(CIString("x-user-name")) match
      case Some(headers) =>
        authController
          .tokenize(Account(User(UserId.newId, headers.head.value)))
          .map(token => Json.obj("authToken" -> token.asJson))
          .flatMap(Created(_))

      case None =>
        BadRequest("User name not specified")
  }
