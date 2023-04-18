package bastoni.backend.routes

import bastoni.backend.{Account, AuthController}
import bastoni.domain.logic.GameController
import bastoni.domain.model.{RoomId, User}
import bastoni.domain.view.{ConnectionError, FromPlayer, ToPlayer}
import bastoni.domain.view.FromPlayer.GameCommand
import cats.effect.{Deferred, IO}
import cats.effect.std.Queue
import cats.syntax.traverse.toTraverseOps
import fs2.Pipe
import io.circe.parser.{decode, parse}
import io.circe.syntax.EncoderOps
import org.http4s.{AuthedRoutes, CacheDirective, Headers, HttpRoutes, Response, StaticFile}
import org.http4s.dsl.io.*
import org.http4s.headers.`Cache-Control`
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.{Close, Text}
import org.http4s.Method.GET

import scala.concurrent.duration.DurationInt

object GameControllerRoutes:

  type AuthResult = Either[ConnectionError, Account]

  def routes(
      authController: AuthController[IO],
      gameController: GameController[IO],
      wsBuilder: WebSocketBuilder2[IO]
  ): HttpRoutes[IO] =
    def send(loginResult: Deferred[IO, AuthResult])(roomId: RoomId): fs2.Stream[IO, WebSocketFrame] =
      fs2.Stream
        .eval(loginResult.get)
        .flatMap[IO, ToPlayer] {
          case Right(account) =>
            fs2.Stream(ToPlayer.Authenticated(account.user)) ++ gameController
              .subscribe(account.user, roomId)
              .merge(fs2.Stream.awakeEvery[IO](5.seconds).map(_ => ToPlayer.Ping))
          case Left(error) =>
            fs2.Stream(ToPlayer.Disconnected(error))
        }
        .map(_.asJson.noSpaces)
        .map(Text(_))
        ++ fs2.Stream.eval(IO.fromEither(Close(1000)))

    def receive(loginResult: Deferred[IO, AuthResult])(roomId: RoomId): Pipe[IO, WebSocketFrame, Unit] =
      (frame: fs2.Stream[IO, WebSocketFrame]) =>
        frame
          .collect { case Text(s, _) => decode[FromPlayer](s) }
          .evalMap(IO.fromEither)
          .evalScan(Option.empty[User]) {
            case (None, FromPlayer.Authenticate(token)) =>
              for
                authentication <- authController.authenticate(token)
                _              <- loginResult.complete(authentication)
              yield authentication.toOption.map(_.user)
            case (None, _) =>
              loginResult.complete(Left(ConnectionError.Forbidden)) *> IO.pure(None)
            case (Some(user), message: GameCommand) =>
              gameController.publish1(user, roomId)(message) *> IO.pure(Some(user))
            case (user, _) =>
              IO.pure(user)
          }
          .as(())

    HttpRoutes.of[IO] { case GET -> Root / UUIDVar(targetRoom) =>
      for
        authResult <- Deferred[IO, AuthResult]
        ws <- wsBuilder.build(
          send = send(authResult)(RoomId(targetRoom)),
          receive = receive(authResult)(RoomId(targetRoom))
        )
      yield ws
    }
  end routes
end GameControllerRoutes
