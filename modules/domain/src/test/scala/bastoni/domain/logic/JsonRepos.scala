package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.PotentiallyDelayed.{decoder, messageEncoder}
import bastoni.domain.repos.*
import cats.effect.{Concurrent, IO, Ref}
import cats.effect.kernel.Async
import cats.syntax.all.*
import cats.Show
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps
import org.typelevel.log4cats.Logger

class JsonKeyValueRepo[
    F[_]: Async: Logger,
    K: Show,
    V: Decoder: Encoder
](data: Ref[F, Map[K, Json]])
    extends KeyValueRepo[F, K, V]:

  override def get(key: K): F[Option[V]] =
    data.get
      .flatMap(entries => Async[F].fromEither(entries.get(key).traverse(Decoder[V].decodeJson)))
      .flatTap {
        case None =>
          Logger[F].debug(show"JsonRepo: No entry found for $key")
        case Some(entry) =>
          Logger[F].debug(show"JsonRepo: Entry found for $key: ${entry.asJson.noSpaces}")
      }

  override def set(key: K, value: V): F[Unit] =
    val json = Encoder[V].apply(value)
    data.update(_ + (key -> json)) <* Logger[F].info(show"JsonRepo: Set $key: $json")

  override def remove(key: K): F[Unit] = data.update(_ - key)

class JsonMessageRepo[F[_]: Concurrent](messages: Ref[F, Map[MessageId, Json]]) extends MessageRepo[F]:
  private val decodeMessage = (Decoder[PotentiallyDelayed[Message]].decodeJson _).andThen {
    case Right(done) => done
    case Left(error) => throw error
  }
  private val encodeMessage = Encoder[PotentiallyDelayed[Message]]

  def flying(message: Message | Delayed[Message]): F[Unit] =
    messages.update(_ + (message match
      case message @ Message(id, _, _)             => id -> encodeMessage(message)
      case delayed @ Delayed(Message(id, _, _), _) => id -> encodeMessage(delayed)
    ))

  def landed(messageId: MessageId): F[Unit] =
    messages.update(_ - messageId)

  val inFlight: fs2.Stream[F, Message | Delayed[Message]] =
    fs2.Stream
      .eval(messages.get)
      .map(_.values.map(decodeMessage))
      .flatMap(fs2.Stream.iterable)

object JsonRepos:

  def gameRepo(using Logger[IO]): IO[GameRepo[IO]] =
    Ref.of[IO, Map[RoomId, Json]](Map.empty).map(new JsonKeyValueRepo[IO, RoomId, GameContext](_))

  def messageRepo(using Logger[IO]): IO[MessageRepo[IO]] =
    Ref.of[IO, Map[MessageId, Json]](Map.empty).map(new JsonMessageRepo[IO](_))
