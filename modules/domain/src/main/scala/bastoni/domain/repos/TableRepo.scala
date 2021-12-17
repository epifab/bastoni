package bastoni.domain.repos

import bastoni.domain.model.{RoomId, ServerTableView}
import cats.effect.Concurrent

type TableRepo[F[_]] = KeyValueRepo[F, RoomId, ServerTableView]

object TableRepo:
  def inMemory[F[_]: Concurrent]: F[TableRepo[F]] = KeyValueRepo.inMemory
