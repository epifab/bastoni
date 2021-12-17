package bastoni.domain.repos

import bastoni.domain.logic.GameContext
import bastoni.domain.model.RoomId
import cats.effect.Concurrent

type GameRepo[F[_]] = KeyValueRepo[F, RoomId, GameContext]

object GameRepo:
  def inMemory[F[_]: Concurrent]: F[GameRepo[F]] = KeyValueRepo.inMemory[F, RoomId, GameContext]
