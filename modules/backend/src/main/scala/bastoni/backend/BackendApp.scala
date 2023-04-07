package bastoni.backend

import bastoni.backend.routes.{GameControllerRoute, StaticResourceRoute, WebHtmlRoute}
import bastoni.domain.logic.{GameController, MessageBus, Services}
import bastoni.domain.model.User
import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
import fs2.concurrent.Signal
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server, ServerBuilder}
import org.http4s.server.middleware.GZip
import org.http4s.server.websocket.WebSocketBuilder2

object BackendApp extends IOApp:

  private def webServer(gameController: GameController[IO]): Resource[IO, Server] =
    BlazeServerBuilder[IO]
      .withHttpWebSocketApp((webSocket: WebSocketBuilder2[IO]) =>
        GZip(
          Router[IO](
            "/assets" -> StaticResourceRoute("assets"),
            "/static" -> StaticResourceRoute("static"),
            "/"       -> WebHtmlRoute("LOCAL"),
            "/play"   -> Account.middleware(GameControllerRoute(gameController, webSocket))
          )
        ).orNotFound
      )
      .bindHttp(9090)
      .resource

  val resource: Resource[IO, fs2.Stream[IO, Unit]] =
    Services.inMemory[IO].flatMap { case (gameController, runner) =>
      webServer(gameController).map(_ => runner)
    }

  override def run(args: List[String]): IO[ExitCode] =
    resource.use(_.compile.drain.as(ExitCode.Success))
