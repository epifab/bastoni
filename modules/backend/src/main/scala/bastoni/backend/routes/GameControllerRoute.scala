package bastoni.backend.routes

import bastoni.backend.Account
import bastoni.domain.logic.GameController
import bastoni.domain.model.{RoomId, User}
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.IO
import fs2.Pipe
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.http4s.{AuthedRoutes, CacheDirective, Headers, HttpRoutes, Response, StaticFile}
import org.http4s.dsl.io.*
import org.http4s.headers.`Cache-Control`
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.Method.GET

import scala.concurrent.duration.DurationInt

object GameControllerRoute:
  def apply(gameController: GameController[IO], wsBuilder: WebSocketBuilder2[IO]): AuthedRoutes[Account, IO] =
    def send(user: User, roomId: RoomId): fs2.Stream[IO, WebSocketFrame] =
      gameController
        .subscribe(user, roomId)
        .merge(fs2.Stream.awakeEvery[IO](5.seconds).map(_ => ToPlayer.Ping))
        .map(_.asJson.noSpaces)
        .map(Text(_))

    def receive(user: User, roomId: RoomId): Pipe[IO, WebSocketFrame, Unit] =
      frame =>
        gameController
          .publish(user, roomId)(
            frame
              .collect { case Text(s, _) => decode[FromPlayer](s) }
              .collect { case Right(message) => message }
          )

    AuthedRoutes.of[Account, IO] { case GET -> Root / UUIDVar(targetRoom) / "play" as account =>
      wsBuilder.build(
        send = send(account.user, RoomId(targetRoom)),
        receive = receive(account.user, RoomId(targetRoom))
      )
    }
