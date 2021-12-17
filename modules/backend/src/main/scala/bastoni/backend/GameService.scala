package bastoni.backend

import bastoni.backend.briscola
import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
import cats.effect.IO

import scala.concurrent.duration.*

enum Delay:
  case Short, Medium, Long

object Delay:
  def defaultDuration: Delay => FiniteDuration = {
    case Delay.Short => 500.millis
    case Delay.Medium => 1.second
    case Delay.Long => 3.seconds
  }

case class DelayedCommand(command: Command, delay: Delay)
case class DelayedMessage(message: Message, delay: Delay)

extension (command: Command)
  def shortly: DelayedCommand = DelayedCommand(command, Delay.Short)
  def later: DelayedCommand = DelayedCommand(command, Delay.Medium)
  def muchLater: DelayedCommand = DelayedCommand(command, Delay.Long)

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
        case ((stateMachines, _), Message(roomId, StartGame(room, gameType))) if !stateMachines.contains(roomId) =>
          (stateMachines + (roomId -> stateMachineFor(room.players, gameType))) -> List(Message(roomId, GameStarted(gameType)))

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

  def stateMachineFor(players: List[Player], gameType: GameType): GameStateMachine =
    gameType match
      case GameType.Briscola => briscola.StateMachine(players)
      case GameType.Tressette => tressette.StateMachine(players)

  def run(messageBus: MessageBus[IO], delayDuration: Delay => FiniteDuration = Delay.defaultDuration): fs2.Stream[IO, Unit] =
    messageBus
      .subscribe
      .through(apply)
      .evalTap {
        case DelayedMessage(message, delay) => messageBus.publish1(message).delayBy(delayDuration(delay)).start
        case message: Message => messageBus.publish1(message)
      }.map(_ => ())
