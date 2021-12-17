package bastoni.backend.routes

import cats.effect.IO
import org.http4s.Method.GET
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityEncoder, HttpRoutes, MediaType}

import scala.xml.Elem

object WebHtmlRoute:

  case class Html(xml: Elem)

  given EntityEncoder[IO, Html] =
    EntityEncoder[IO, String]
      .contramap[Html](html => "<!DOCTYPE html>\n" + html.xml.mkString)
      .withContentType(`Content-Type`(MediaType.text.html))

  def apply(appVersion: String): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req@GET -> Root => Ok(
        Html(
          <html>
            <head>
              <title>Controbuio</title>
              <link rel="stylesheet" href={s"/static/styles.css?v=$appVersion"} />
              <link rel="shortcut icon" href={s"/static/denari.svg?v=$appVersion"} type="image/svg+xml" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            </head>
            <body>
              <h1>Controbuio</h1>
              <div id="app-wrapper">Loading web app</div>
              <script src={s"/assets/frontend-jsdeps.js?v=$appVersion"}></script>
              <script src={s"/assets/frontend-opt/main.js?v=$appVersion"}></script>
            </body>
          </html>
        )
      )
    }
