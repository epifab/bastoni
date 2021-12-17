package bastoni.backend

import bastoni.domain.model.{Message, MessageId, RoomId}

type GameRooms = Map[RoomId, GameStateMachine]
type Messages  = Map[MessageId, Message | Delayed[Message]]

trait GameServiceRepo[F[_]]:
  def getSnapshot: F[(GameRooms, Messages)]
  def setSnapshot(snapshot: GameRooms, inFlight: Messages): F[Unit]
