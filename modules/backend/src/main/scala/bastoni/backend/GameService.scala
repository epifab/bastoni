package bastoni.backend

import bastoni.backend.briscola
import bastoni.domain.model.*
import cats.effect.IO

import scala.concurrent.duration.*

enum Delay(val duration: FiniteDuration):
  case Short extends Delay(500.millis)
  case Medium extends Delay(1.second)
  case Long extends Delay(3.seconds)

case class DelayedCommand(command: Command, delay: Delay)
case class DelayedMessage(message: Message, delay: Delay)

extension (command: Command)
  def shortly: DelayedCommand = DelayedCommand(command, Delay.Short)
  def delayed: DelayedCommand = DelayedCommand(command, Delay.Medium)
  def veryDelayed: DelayedCommand = DelayedCommand(command, Delay.Long)

extension (message: Command | DelayedCommand | Event)
  def toMessage(roomId: RoomId): Message | DelayedMessage = message match
    case DelayedCommand(command, delay) => DelayedMessage(Message(roomId, command), delay)
    case command: Command => Message(roomId, command)
    case event: Event => Message(roomId, event)

trait GameStateMachine extends ((Event | Command) => (Option[GameStateMachine], List[Command | DelayedCommand | Event]))

object GameService:
  def apply[F[_]](messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | DelayedMessage] =
    messages
      .scan[(Map[RoomId, GameStateMachine], List[Message | DelayedMessage])](Map.empty -> Nil) {
        case ((stateMachines, _), Message(roomId, StartGame(room, GameType.Briscola))) if !stateMachines.contains(roomId) =>
          (stateMachines + (roomId -> briscola.StateMachine(room.players))) -> Nil

        case ((stateMachines, _), Message(roomId, message)) =>
          stateMachines.get(roomId).fold(stateMachines -> Nil) { stateMachine =>
            stateMachine(message) match
              case (Some(newStateMachine), events) =>
                (stateMachines + (roomId -> newStateMachine)) -> events.map(_.toMessage(roomId))
              case (None, events) =>
                (stateMachines - roomId) -> events.map(_.toMessage(roomId))
          }
      }
      .flatMap { case (_, messages) => fs2.Stream.iterable(messages) }

  def run(messageBus: MessageBus[IO]): fs2.Stream[IO, Unit] =
    messageBus
      .subscribe
      .through(apply)
      .evalTap {
        case DelayedMessage(message, delay) => messageBus.publish1(message).delayBy(delay.duration).start
        case message: Message => messageBus.publish1(message)
      }.map(_ => ())
