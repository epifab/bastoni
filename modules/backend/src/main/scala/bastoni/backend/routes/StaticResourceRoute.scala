package bastoni.backend.routes

import cats.effect.IO
import org.http4s.{CacheDirective, Headers, HttpRoutes, Response, StaticFile}
import org.http4s.dsl.io.*
import org.http4s.headers.`Cache-Control`
import org.http4s.Method.GET

import scala.concurrent.duration.DurationInt

object StaticResourceRoute:

  object VersionQueryParam extends OptionalQueryParamDecoderMatcher[String]("v")

  def apply(baseDir: String): HttpRoutes[IO] = HttpRoutes.of[IO] { case GET -> path :? VersionQueryParam(version) =>
    resource(baseDir, path.toString, version.nonEmpty && !version.contains("LOCAL"))
  }

  private def resource(baseDir: String, path: String, longCache: Boolean): IO[Response[IO]] =
    StaticFile
      .fromResource[IO](s"/$baseDir/$path")
      .map(r =>
        r.withHeaders(
          r.headers ++ Headers.of(
            `Cache-Control`(
              CacheDirective.public,
              CacheDirective.`max-age`(if (!longCache) 10.seconds else 7.days)
            )
          )
        )
      )
      .getOrElseF(NotFound("Resource not found"))
