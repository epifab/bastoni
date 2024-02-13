package bastoni.backend

import bastoni.backend.routes.{AuthControllerRoutes, GameControllerRoutes, StaticResourceRoute, WebHtmlRoute}
import bastoni.domain.logic.{GameController, Services}
import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.server.middleware.GZip
import org.http4s.server.websocket.WebSocketBuilder2
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

object BackendApp extends IOApp:

  given Logger[IO] = Slf4jLogger.getLogger

  private def webServer(gameController: GameController[IO]): Resource[IO, Server] =
    BlazeServerBuilder[IO]
      .withHttpWebSocketApp((webSocket: WebSocketBuilder2[IO]) =>
        GZip(
          Router[IO](
            "/assets" -> StaticResourceRoute("assets"),
            "/static" -> StaticResourceRoute("static"),
            "/"       -> WebHtmlRoute("LOCAL"),
            "/auth"   -> AuthControllerRoutes.routes(InsecureAuthController),
            "/play"   -> GameControllerRoutes.routes(InsecureAuthController, gameController, webSocket)
          )
        ).orNotFound,
      )
      .bindHttp(9090)
      .resource

  val resource: Resource[IO, fs2.Stream[IO, Unit]] =
    Services.inMemory[IO].flatMap { case (gameController, runner) =>
      webServer(gameController).map(_ => runner)
    }

  override def run(args: List[String]): IO[ExitCode] =
    resource.use(_.compile.drain.as(ExitCode.Success))
