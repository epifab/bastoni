package bastoni.domain.logic

import bastoni.domain.model.{Delayed, Message, MessageId, RoomId}
import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*

type GameRooms = Map[RoomId, GameStateMachine]
type Messages  = Map[MessageId, Message | Delayed[Message]]

class InMemoryGameServiceRepo[F[_]: Concurrent](val gameRooms: Ref[F, GameRooms], val messages: Ref[F, Messages]) extends GameServiceRepo[F]:

  def get(roomId: RoomId): F[Option[GameStateMachine]] = gameRooms.get.map(_.get(roomId))
  def set(roomId: RoomId, stateMachine: GameStateMachine): F[Unit] = gameRooms.update(_ + (roomId -> stateMachine))
  def remove(roomId: RoomId): F[Unit] = gameRooms.update(_ - roomId)

  def flying(message: Message | Delayed[Message]): F[Unit] =
    messages.update(_ + (message match {
      case message@ Message(id, _, _)             => id -> message
      case delayed@ Delayed(Message(id, _, _), _) => id -> delayed
    }))

  def landed(messageId: MessageId): F[Unit] = messages.update(_ - messageId)

  val inFlight: fs2.Stream[F, Message | Delayed[Message]] =
    fs2.Stream
      .eval(messages.get)
      .flatMap { case messages => fs2.Stream.iterable(messages.values) }


object InMemoryGameServiceRepo:
  def apply[F[_]: Concurrent] =
    for {
      gameRooms <- Ref.of[F, GameRooms](Map.empty)
      messages <- Ref.of[F, Messages](Map.empty)
    } yield new InMemoryGameServiceRepo[F](gameRooms, messages)
