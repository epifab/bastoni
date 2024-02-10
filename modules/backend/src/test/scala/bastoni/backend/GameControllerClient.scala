package bastoni.backend

import bastoni.domain.model.RoomId
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.kernel.{Async, Resource, Sync}
import cats.syntax.all.*
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.http4s.client.websocket.{WSClient, WSFrame, WSRequest}
import org.http4s.jdkhttpclient.JdkWSClient
import org.http4s.Uri

trait GameControllerClient[F[_]]:
  def send(messages: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit]
  def receive: fs2.Stream[F, ToPlayer]

  def send1(message: FromPlayer): F[Unit]
  def receive1: F[ToPlayer]
  def askTo(message: FromPlayer): F[ToPlayer]

class GameControllerClientBuilder[F[_]: Sync](client: WSClient[F], baseUri: Uri):

  def connect(roomId: RoomId): Resource[F, GameControllerClient[F]] =
    client
      .connect(WSRequest(baseUri / "play" / roomId.value))
      .map(connection =>
        new GameControllerClient[F]:
          override def send(messages: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
            messages.evalMap(send1)

          override def receive: fs2.Stream[F, ToPlayer] =
            connection.receiveStream
              .collect { case frame: WSFrame.Text => decode[ToPlayer](frame.data) }
              .evalMap(Sync[F].fromEither)

          override def send1(message: FromPlayer): F[Unit] =
            connection.send(WSFrame.Text(message.asJson.noSpaces))

          override def receive1: F[ToPlayer] =
            receive.head.compile.lastOrError

          override def askTo(message: FromPlayer): F[ToPlayer] =
            send1(message) *> receive1
      )

object GameControllerClientBuilder:
  def apply[F[_]: Async](baseUri: Uri): Resource[F, GameControllerClientBuilder[F]] =
    Resource
      .eval(JdkWSClient.simple[F])
      .map(client => new GameControllerClientBuilder(client, baseUri))
