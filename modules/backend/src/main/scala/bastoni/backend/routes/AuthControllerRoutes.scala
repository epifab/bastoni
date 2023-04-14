package bastoni.backend.routes

import bastoni.domain.model.{User, UserId}
import cats.effect.IO
import io.circe.syntax.EncoderOps
import org.http4s.{HttpRoutes, ResponseCookie}
import org.http4s.dsl.io.*
import org.typelevel.ci.CIString

object AuthControllerRoutes:
  val routes: HttpRoutes[IO] = HttpRoutes.of { case req @ POST -> Root =>
    req.cookies.find(_.name == "auth") match
      case Some(_) => NoContent()
      case None =>
        req.headers.get(CIString("x-user-name")) match
          case Some(headers) =>
            Created().map(
              _.addCookie(
                ResponseCookie(
                  "auth",
                  User(UserId.newId, headers.head.value).asJson.noSpaces,
                  path = Some("/"),
                  httpOnly = true
                )
              )
            )
          case None =>
            BadRequest("User name not specified")
  }
