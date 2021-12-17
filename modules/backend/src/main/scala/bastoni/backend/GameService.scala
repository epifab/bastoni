package bastoni.backend

import bastoni.backend.briscola
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import cats.{Functor, Monad}
import cats.effect.{Async, Concurrent}
import cats.effect.syntax.all.*
import cats.syntax.functor.toFunctorOps

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

/**
 * The game service state
 * @param snapshot Snapshot of all active state machines
 * @param currentlyInFlight A set of messages that were previously produced but not yet consumed by the GameService
 * @param nextToFly The messages produced after by the latest iteration
 */
case class GameServiceState(
  snapshot: GameRooms,
  currentlyInFlight: Messages,
  nextToFly: List[Message | Delayed[Message]]
):
  def contains(roomId: RoomId): Boolean = snapshot.contains(roomId)
  def get(roomId: RoomId): Option[GameStateMachine] = snapshot.get(roomId)
  def set(roomId: RoomId, stateMachine: GameStateMachine): GameServiceState = copy(snapshot = snapshot + (roomId -> stateMachine))
  def remove(roomId: RoomId): GameServiceState = copy(snapshot = snapshot - roomId)

  def fly(messages: List[Message | Delayed[Message]]): GameServiceState =
    copy(
      nextToFly = messages,
      currentlyInFlight = (currentlyInFlight ++ messages.map {
        case delayed@ Delayed(message: Message, _) => message.id -> delayed
        case message: Message => message.id -> message
      })
    )

  def landed(messageId: MessageId): GameServiceState =
    copy(
      currentlyInFlight = currentlyInFlight - messageId,
      nextToFly = Nil
    )


object GameService:

  def apply[F[_]: Concurrent](messageIds: fs2.Stream[F, MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    runStateMachines(messages, messageIds)
      .flatMap { case state => fs2.Stream.iterable(state.nextToFly) }

  def apply[F[_]: Concurrent](messageIds: fs2.Stream[F, MessageId], gameServiceRepo: GameServiceRepo[F])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    for {
      state <- fs2.Stream.eval(gameServiceRepo.getSnapshot)
      (snapshot, inFlight) = state
      oldEvents = fs2.Stream.iterable[F, Message | Delayed[Message]](inFlight.values)
      newEvents = runStateMachines(messages, messageIds, snapshot, inFlight)
        .evalTap { case state => gameServiceRepo.setSnapshot(state.snapshot, state.currentlyInFlight) }
        .flatMap { case state => fs2.Stream.iterable(state.nextToFly) }
      event <- oldEvents ++ newEvents
    } yield event

  private def runStateMachines[F[_]: Concurrent](
    messages: fs2.Stream[F, Message],
    messageIds: fs2.Stream[F, MessageId],
    initialSnapshot: GameRooms = Map.empty,
    initialInFlight: Messages = Map.empty
  ): fs2.Stream[F, GameServiceState] =
    messages
      .evalScan[F, GameServiceState](GameServiceState(initialSnapshot, initialInFlight, Nil)) { case (state, message) =>
        (state.landed(message.id), message) match {
          case (state, Message(_, roomId, StartGame(room, gameType))) if !state.contains(roomId) =>
            List(GameStarted(gameType))
              .toMessages(roomId, messageIds)
              .compile.toList
              .map(state.set(roomId, stateMachineFor(room.players, gameType)).fly _)

          case (state, Message(_, roomId, message)) =>
            state.get(roomId).fold(Monad[F].pure(state)) { stateMachine =>
              val (newStateMachine, events) = stateMachine(message)
              events
                .toMessages(roomId, messageIds)
                .compile.toList
                .map(newStateMachine.fold(state.remove(roomId))(state.set(roomId, _)).fly _)
            }
        }
      }

  def stateMachineFor(players: List[Player], gameType: GameType): GameStateMachine =
    gameType match
      case GameType.Briscola => briscola.StateMachine(players)
      case GameType.Tressette => tressette.StateMachine(players)

  def run[F[_]: Async](
    messageBus: MessageBus[F],
    repo: GameServiceRepo[F],
    delayDuration: Delay => FiniteDuration = Delay.defaultDuration
  ): fs2.Stream[F, Unit] =
    messageBus
      .subscribe
      .through(apply(fs2.Stream.repeatEval(Async[F].delay(MessageId.newId)), repo))
      .evalMap {
        case Delayed(message, delay) => messageBus.publish1(message).delayBy(delayDuration(delay)).start.void
        case message: Message => messageBus.publish1(message)
      }
