package bastoni.backend

import bastoni.backend.briscola
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import cats.Functor
import cats.syntax.functor.toFunctorOps
import cats.effect.Async
import cats.effect.syntax.all.*

import scala.concurrent.duration.*

enum Delay:
  case Short, Medium, Long

object Delay:
  def defaultDuration: Delay => FiniteDuration = {
    case Delay.Short => 500.millis
    case Delay.Medium => 1.second
    case Delay.Long => 3.seconds
  }

case class Delayed[T](inner: T, delay: Delay):
  def map[U](f: T => U): Delayed[U] = Delayed(f(inner), delay)

extension (command: Command)
  def shortly: Delayed[Command] = Delayed(command, Delay.Short)
  def later: Delayed[Command] = Delayed(command, Delay.Medium)
  def muchLater: Delayed[Command] = Delayed(command, Delay.Long)

trait GameStateMachine extends ((Event | Command) => (Option[GameStateMachine], List[Command | Delayed[Command] | Event]))

extension (messages: List[Command | Delayed[Command] | Event])
  def toMessages[F[_]](roomId: RoomId, messageIds: fs2.Stream[F, MessageId]): fs2.Stream[F, Message | Delayed[Message]] =
    fs2.Stream
      .iterable(messages)
      .zip(messageIds)
      .map {
        case (event: Event, id) => Message(id, roomId, event)
        case (command: Command, id) => Message(id, roomId, command)
        case (Delayed(command: Command, delay), id) => Delayed(Message(id, roomId, command), delay)
      }

object GameService:

  def apply[F[_]](messageIds: fs2.Stream[F, MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    runStateMachines(messages, messageIds)
      .flatMap { case (_, messages) => messages }

  def apply[F[_]: Functor](messageIds: fs2.Stream[F, MessageId], gameServiceRepo: GameServiceRepo[F])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    fs2.Stream.eval(gameServiceRepo.getSnapshot).flatMap { initialState =>
      runStateMachines(messages, messageIds, initialState)
        .evalTap { case (stateMachines, _) => gameServiceRepo.setSnapshot(stateMachines) }
        .flatMap { case (_, messages) => messages }
    }

  private def runStateMachines[F[_]](
    messages: fs2.Stream[F, Message],
    messageIds: fs2.Stream[F, MessageId],
    initialState: Map[RoomId, GameStateMachine] = Map.empty
  ): fs2.Stream[F, (Map[RoomId, GameStateMachine], fs2.Stream[F, Message | Delayed[Message]])] =
    messages
      .scan[(Map[RoomId, GameStateMachine], fs2.Stream[F, Message | Delayed[Message]])](Map.empty -> fs2.Stream.empty) {
        case ((stateMachines, _), Message(_, roomId, StartGame(room, gameType))) if !stateMachines.contains(roomId) =>
          (stateMachines + (roomId -> stateMachineFor(room.players, gameType))) -> List(GameStarted(gameType)).toMessages(roomId, messageIds)

        case ((stateMachines, _), Message(_, roomId, message)) =>
          stateMachines.get(roomId).fold(stateMachines -> fs2.Stream.empty) { stateMachine =>
            stateMachine(message) match
              case (Some(newStateMachine), events) =>
                (stateMachines + (roomId -> newStateMachine)) -> events.toMessages(roomId, messageIds)
              case (None, events) =>
                (stateMachines - roomId) -> events.toMessages(roomId, messageIds)
          }
      }

  def stateMachineFor(players: List[Player], gameType: GameType): GameStateMachine =
    gameType match
      case GameType.Briscola => briscola.StateMachine(players)
      case GameType.Tressette => tressette.StateMachine(players)

  def run[F[_]: Async](
    messageBus: MessageBus[F],
    delayDuration: Delay => FiniteDuration = Delay.defaultDuration
  ): fs2.Stream[F, Unit] =
    messageBus
      .subscribe
      .through(apply(fs2.Stream.repeatEval(Async[F].delay(MessageId.newId))))
      .evalMap {
        case Delayed(message, delay) => messageBus.publish1(message).delayBy(delayDuration(delay)).start.void
        case message: Message => messageBus.publish1(message)
      }
