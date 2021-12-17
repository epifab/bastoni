package bastoni.backend

import bastoni.domain.model.RoomId

trait GameServiceRepo[F[_]]:
  def getSnapshot: F[Map[RoomId, GameStateMachine]]
  def setSnapshot(snapshot: Map[RoomId, GameStateMachine]): F[Unit]
