package bastoni.backend

import bastoni.domain.model.{Delayed, Message, MessageId, RoomId}

trait GameServiceRepo[F[_]]:
  def get(roomId: RoomId): F[Option[GameStateMachine]]
  def set(roomId: RoomId, stateMachine: GameStateMachine): F[Unit]
  def remove(roomId: RoomId): F[Unit]

  def flying(message: Message | Delayed[Message]): F[Unit]
  def landed(messageId: MessageId): F[Unit]
  def inFlight: fs2.Stream[F, Message | Delayed[Message]]
