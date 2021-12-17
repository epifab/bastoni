package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.PotentiallyDelayed.{decoder, messageEncoder}
import bastoni.domain.repos.{GameRepo, MessageRepo, RoomRepo, KeyValueRepo}
import cats.effect.syntax.all.*
import cats.effect.{Concurrent, IO, Ref}
import cats.syntax.all.*
import io.circe.{Decoder, Encoder, Json}

class JsonKeyValueRepo[F[_]: Concurrent, K, V: Decoder: Encoder](data: Ref[F, Map[K, Json]]) extends KeyValueRepo[F, K, V]:
  override def get(key: K): F[Option[V]] =
    data.get
      .map(_.get(key).map((Decoder[V].decodeJson _).andThen {
        case Right(v) => v
        case Left(error) => throw error
      }))

  override def set(key: K, value: V): F[Unit] =
    data.update(_ + (key -> Encoder[V].apply(value)))

  override def remove(key: K): F[Unit] = data.update(_ - key)


class JsonMessageRepo[F[_]: Concurrent](messages: Ref[F, Map[MessageId, Json]]) extends MessageRepo[F]:
  private val decodeMessage = (Decoder[PotentiallyDelayed[Message]].decodeJson _).andThen {
    case Right(done) => done
    case Left(error) => throw error
  }
  private val encodeMessage = Encoder[PotentiallyDelayed[Message]]

  def flying(message: Message | Delayed[Message]): F[Unit] =
    messages.update(_ + (message match {
      case message@ Message(id, _, _)             => id -> encodeMessage(message)
      case delayed@ Delayed(Message(id, _, _), _) => id -> encodeMessage(delayed)
    }))

  def landed(messageId: MessageId): F[Unit] =
    messages.update(_ - messageId)

  val inFlight: fs2.Stream[F, Message | Delayed[Message]] =
    fs2.Stream
      .eval(messages.get)
      .map(_.values.map(decodeMessage))
      .flatMap(fs2.Stream.iterable)


object JsonRepos:
  val gameRepo: IO[GameRepo[IO]] = Ref.of[IO, Map[RoomId, Json]](Map.empty).map(new JsonKeyValueRepo[IO, RoomId, GameRoom](_))
  val roomRepo: IO[RoomRepo[IO]] = Ref.of[IO, Map[RoomId, Json]](Map.empty).map(new JsonKeyValueRepo[IO, RoomId, Room](_))
  val messageRepo: IO[MessageRepo[IO]] = Ref.of[IO, Map[MessageId, Json]](Map.empty).map(new JsonMessageRepo[IO](_))
