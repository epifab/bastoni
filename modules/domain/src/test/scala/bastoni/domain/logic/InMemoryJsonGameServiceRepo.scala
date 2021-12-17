package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.PotentiallyDelayed.{decoder, messageEncoder}
import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*
import io.circe.{Decoder, Encoder, Json}

class InMemoryJsonGameServiceRepo[F[_]: Concurrent](val gameRooms: Ref[F, Map[RoomId, Json]], val messages: Ref[F, Map[MessageId, Json]]) extends GameServiceRepo[F]:

  val decodeGameMachine = (Decoder[GameStateMachine].decodeJson _).andThen {
    case Right(done) => done
    case Left(error) => throw error
  }
  val encodeGameMachine = Encoder[GameStateMachine]

  val decodeMessage = (Decoder[PotentiallyDelayed[Message]].decodeJson _).andThen {
    case Right(done) => done
    case Left(error) => throw error
  }
  val encodeMessage = Encoder[PotentiallyDelayed[Message]]

  def get(roomId: RoomId): F[Option[GameStateMachine]] =
    gameRooms
      .get
      .map(_.get(roomId))
      .map(_.map(decodeGameMachine))

  def set(roomId: RoomId, stateMachine: GameStateMachine): F[Unit] =
    gameRooms.update(_ + (roomId -> encodeGameMachine(stateMachine)))

  def remove(roomId: RoomId): F[Unit] =
    gameRooms.update(_ - roomId)

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


object InMemoryJsonGameServiceRepo:
  def apply[F[_]: Concurrent]: F[InMemoryJsonGameServiceRepo[F]] =
    for {
      gameRooms <- Ref.of[F, Map[RoomId, Json]](Map.empty)
      messages <- Ref.of[F, Map[MessageId, Json]](Map.empty)
    } yield new InMemoryJsonGameServiceRepo[F](gameRooms, messages)
