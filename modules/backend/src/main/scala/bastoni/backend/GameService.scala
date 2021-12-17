package bastoni.backend

import bastoni.backend.briscola
import bastoni.domain.*

trait GameStateMachine extends ((Event | Command) => (Option[GameStateMachine], List[Event]))

object GameService:
  def apply[F[_]](messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .scan[(Map[RoomId, GameStateMachine], List[Message])](Map.empty -> Nil) {
        case ((stateMachines, _), Message(roomId, StartGame(room, GameType.Briscola))) if !stateMachines.contains(roomId) =>
          (stateMachines + (roomId -> briscola.StateMachine(room.players))) -> Nil

        case ((stateMachines, _), Message(roomId, message)) =>
          stateMachines.get(roomId).fold(stateMachines -> Nil) { stateMachine =>
            stateMachine(message) match
              case (Some(newStateMachine), events) =>
                (stateMachines + (roomId -> newStateMachine)) -> events.map(Message(roomId, _))
              case (None, events) =>
                (stateMachines - roomId) -> events.map(Message(roomId, _))
          }
      }
      .flatMap { case (_, messages) => fs2.Stream.iterable(messages) }
