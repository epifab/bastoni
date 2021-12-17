package bastoni.backend

import bastoni.domain.model.{Message, MessageId, RoomId}

trait GameServiceRepo[F[_]]:
  def getSnapshot: F[(Map[RoomId, GameStateMachine], Map[MessageId, Message | Delayed[Message]])]
  def setSnapshot(snapshot: Map[RoomId, GameStateMachine], inFlight: Map[MessageId, Message | Delayed[Message]]): F[Unit]
