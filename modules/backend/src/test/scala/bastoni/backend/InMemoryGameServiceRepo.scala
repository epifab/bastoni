package bastoni.backend

import bastoni.domain.model.{Message, MessageId, RoomId}
import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*

class InMemoryGameServiceRepo[F[_]: Concurrent](stateRef: Ref[F, (GameRooms, Messages)]) extends GameServiceRepo[F]:
  def getSnapshot: F[(GameRooms, Messages)] = stateRef.get
  def setSnapshot(snapshot: GameRooms, inFlight: Messages): F[Unit] = stateRef.set((snapshot, inFlight))

object InMemoryGameServiceRepo:
  def apply[F[_]: Concurrent] =
    Ref.of[F, (GameRooms, Messages)](Map.empty -> Map.empty)
      .map(new InMemoryGameServiceRepo(_))
