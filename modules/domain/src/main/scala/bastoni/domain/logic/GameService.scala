package bastoni.domain.logic

import bastoni.domain.logic.briscola
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import cats.Applicative
import cats.effect.syntax.all.*
import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import scala.concurrent.duration.*

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
  def apply[F[_]: Concurrent](newId: F[MessageId], repo: GameServiceRepo[F])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    messages
      .evalMap { case Message(id, roomId, data) =>
        for {
          stateMachine <- repo.get(roomId)
          (newStateMachine, messagesData) = (stateMachine, data) match {
            case (None, StartGame(room, gameType)) => Some(GameStateMachineFactory(gameType)(room)) -> List(GameStarted(gameType))
            case (Some(state), event) => state(event)
            case (state, _) => state -> Nil
          }
          messages <- messagesData.toMessages(roomId, newId)
          // the remaining operations should be done atomically to guarantee consistency
          _ <- repo.landed(id)
          _ <- newStateMachine.fold(repo.remove(roomId))(repo.set(roomId, _))
          _ <- messages.traverse(repo.flying)
        } yield messages
      }
      .flatMap(fs2.Stream.iterable)

  def run[F[_]: Async](
    messageBus: MessageBus[F],
    repo: GameServiceRepo[F],
    delayDuration: Delay => FiniteDuration = {
      case Delay.Short => 500.millis
      case Delay.Medium => 1.second
      case Delay.Long => 3.seconds
    }
  ): fs2.Stream[F, Unit] =
    for {
      subscriber <- fs2.Stream.resource(messageBus.subscribeAwait)
      oldMessages = repo.inFlight
      newMessages = subscriber.through(apply(Async[F].delay(MessageId.newId), repo))
      event <- (oldMessages ++ newMessages).evalMap {
        case Delayed(message, delay) => messageBus.publish1(message).delayBy(delayDuration(delay)).start.void
        case message: Message        => messageBus.publish1(message)
      }
    } yield ()

