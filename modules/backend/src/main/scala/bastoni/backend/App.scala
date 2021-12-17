package bastoni.backend

import bastoni.backend.routes.{StaticResourceRoute, WebHtmlRoute}
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.GZip
import org.http4s.server.{Router, Server, ServerBuilder}

object App extends IOApp:

  val router = Router(
    "/assets" -> StaticResourceRoute("assets"),
    "/static" -> StaticResourceRoute("static"),
    "/" -> WebHtmlRoute(1)
  )

  val webServer: ServerBuilder[IO] = {
    BlazeServerBuilder[IO]
      .withHttpApp(GZip(router).orNotFound)
      .bindHttp(9001, "0.0.0.0")
  }

  override def run(args: List[String]): IO[ExitCode] =
    webServer
      .serve
      .compile
      .drain.as(ExitCode.Success)
