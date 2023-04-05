package bastoni.backend

import bastoni.backend.routes.{GameControllerRoute, StaticResourceRoute, WebHtmlRoute}
import bastoni.domain.logic.{GameController, MessageBus, Services}
import bastoni.domain.model.User
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.{Router, Server, ServerBuilder}
import org.http4s.server.middleware.GZip
import org.http4s.server.websocket.WebSocketBuilder2

object App extends IOApp:

  private def webServer(gameController: GameController[IO]): ServerBuilder[IO] =
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

  override def run(args: List[String]): IO[ExitCode] =
    Services.inMemory[IO].use { case (gameController, runner) =>
      webServer(gameController).serve
        .concurrently(runner)
        .compile
        .drain
        .as(ExitCode.Success)
    }
