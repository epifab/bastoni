package bastoni.backend

import bastoni.backend.briscola
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import cats.Applicative
import cats.effect.{Async, Concurrent}
import cats.effect.syntax.all.*
import cats.syntax.all.*

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

  def toMessages[F[_]: Applicative](roomId: RoomId, newId: F[MessageId]): F[List[Message | Delayed[Message]]] =
    messages
      .traverse {
        case event: Event                     => newId.map(id => Message(id, roomId, event))
        case command: Command                 => newId.map(id => Message(id, roomId, command))
        case Delayed(command: Command, delay) => newId.map(id => Delayed(Message(id, roomId, command), delay))
      }


object GameService:
  private def runStateMachines[F[_]: Concurrent](newId: F[MessageId], repo: GameServiceRepo[F])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    messages
      .evalMap { case Message(id, roomId, data) =>
        for {
          _ <- repo.landed(id)
          stateMachine <- repo.get(roomId)
          (newStateMachine, messagesData) = (stateMachine, data) match {
            case (None, StartGame(room, gameType)) => Some(stateMachineFor(room.players, gameType)) -> List(GameStarted(gameType))
            case (Some(state), event) => state(event)
            case (state, _) => state -> Nil
          }
          _ <- newStateMachine.fold(repo.remove(roomId))(repo.set(roomId, _))
          messages <- messagesData.toMessages(roomId, newId)
          _ <- messages.traverse(repo.flying)
        } yield messages
      }
      .flatMap(fs2.Stream.iterable)

  def stateMachineFor(players: List[Player], gameType: GameType): GameStateMachine =
    gameType match
      case GameType.Briscola => briscola.StateMachine(players)
      case GameType.Tressette => tressette.StateMachine(players)

  def run[F[_]: Async](
    messageBus: MessageBus[F],
    repo: GameServiceRepo[F],
    delayDuration: Delay => FiniteDuration = Delay.defaultDuration
  ): fs2.Stream[F, Unit] =
    (repo.inFlight ++ messageBus.subscribe.through(runStateMachines(Async[F].delay(MessageId.newId), repo)))
      .evalMap {
        case Delayed(message, delay) => messageBus.publish1(message).delayBy(delayDuration(delay)).start.void
        case message: Message => messageBus.publish1(message)
      }
