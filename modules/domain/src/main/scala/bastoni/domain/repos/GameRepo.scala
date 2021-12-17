package bastoni.domain.repos

import bastoni.domain.logic.GameStateMachine
import bastoni.domain.model.{RoomId, Room}
import cats.effect.Concurrent

type GameRepo[F[_]] = KeyValueRepo[F, RoomId, GameStateMachine]

object GameRepo:
  def inMemory[F[_]: Concurrent]: F[GameRepo[F]] = KeyValueRepo.inMemory[F, RoomId, GameStateMachine]
