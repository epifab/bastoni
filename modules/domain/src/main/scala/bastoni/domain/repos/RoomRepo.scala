package bastoni.domain.repos

import bastoni.domain.model.{Room, RoomId}
import cats.effect.Concurrent

type RoomRepo[F[_]] = KeyValueRepo[F, RoomId, Room]

object RoomRepo:
  def inMemory[F[_]: Concurrent]: F[RoomRepo[F]] = KeyValueRepo.inMemory
