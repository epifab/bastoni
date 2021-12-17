package bastoni.domain.repos

import bastoni.domain.model.{RoomId, GameRoom}
import cats.effect.Concurrent

type GameRepo[F[_]] = KeyValueRepo[F, RoomId, GameRoom]

object GameRepo:
  def inMemory[F[_]: Concurrent]: F[GameRepo[F]] = KeyValueRepo.inMemory[F, RoomId, GameRoom]
