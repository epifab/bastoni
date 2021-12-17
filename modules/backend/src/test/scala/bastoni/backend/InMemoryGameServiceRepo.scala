package bastoni.backend

import bastoni.backend.InMemoryGameServiceRepo.{M, S}
import bastoni.domain.model.{Message, MessageId, RoomId}
import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*

class InMemoryGameServiceRepo[F[_]: Concurrent](stateRef: Ref[F, (S, M)]) extends GameServiceRepo[F]:
  def getSnapshot: F[(S, M)] = stateRef.get
  def setSnapshot(snapshot: S, inFlight: M): F[Unit] = stateRef.set((snapshot, inFlight))

object InMemoryGameServiceRepo:
  type S = Map[RoomId, GameStateMachine]
  type M = Map[MessageId, Message | Delayed[Message]]

  def apply[F[_]: Concurrent] =
    Ref.of[F, (S, M)](Map.empty -> Map.empty)
      .map(new InMemoryGameServiceRepo(_))
