package bastoni.domain.repos

import bastoni.domain.model.{RoomId, TableServerView}
import cats.effect.Concurrent

type TableRepo[F[_]] = KeyValueRepo[F, RoomId, TableServerView]

object TableRepo:
  def inMemory[F[_]: Concurrent]: F[TableRepo[F]] = KeyValueRepo.inMemory
