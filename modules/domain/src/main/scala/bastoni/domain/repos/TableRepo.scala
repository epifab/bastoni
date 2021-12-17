package bastoni.domain.repos

import bastoni.domain.model.{RoomId, Table}
import cats.effect.Concurrent

type TableRepo[F[_]] = KeyValueRepo[F, RoomId, Table]

object TableRepo:
  def inMemory[F[_]: Concurrent] = KeyValueRepo.inMemory
