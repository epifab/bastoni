package bastoni.domain.logic

import bastoni.domain.logic.briscola
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.repos.{GameRepo, MessageRepo}
import cats.Applicative
import cats.effect.syntax.all.*
import cats.effect.{Async, Concurrent}
import cats.syntax.all.*
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import scala.concurrent.duration.*

extension (data: List[Command | Delayed[Command] | Event])
  def toMessages[F[_]: Applicative](roomId: RoomId, newId: F[MessageId]): F[List[Message | Delayed[Message]]] =
    data
      .traverse {
        case event: Event                     => newId.map(id => Message(id, roomId, event))
        case command: Command                 => newId.map(id => Message(id, roomId, command))
        case Delayed(command: Command, delay) => newId.map(id => Delayed(Message(id, roomId, command), delay))
      }

object GameService:
  def apply[F[_]: Concurrent](newId: F[MessageId], gameRepo: GameRepo[F], messageRepo: MessageRepo[F])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    messages
      .evalMap { case Message(id, roomId, data) =>
        for {
          existingRoom <- gameRepo.get(roomId)
          (newGameRoom, messagesData) = existingRoom.map(_.update(data)).orElse(GameRoom.build(data)) match {
            case None => None -> Nil
            case Some(room) =>
              val (stateMachine, messagesData) = (room.stateMachine, data) match {
                case (None, StartGame(room, gameType)) => Some(GameStateMachineFactory(gameType)(room)) -> List(GameStarted(gameType))
                case (Some(state), event) => state(event)
                case (None, _) => None -> Nil
              }
              Some(room.withStateMachine(stateMachine)) -> messagesData
          }
          messages <- messagesData.toMessages(roomId, newId)
          // the remaining operations should be done atomically to guarantee consistency
          _ <- messageRepo.landed(id)
          _ <- messages.traverse(messageRepo.flying)
          _ <- newGameRoom.fold(gameRepo.remove(roomId))(gameRepo.set(roomId, _))
        } yield messages
      }
      .flatMap(fs2.Stream.iterable)

  def run[F[_]: Async](
    messageBus: MessageBus[F],
    gameRepo: GameRepo[F],
    messageRepo: MessageRepo[F],
    delayDuration: Delay => FiniteDuration = {
      case Delay.Short => 500.millis
      case Delay.Medium => 1.second
      case Delay.Long => 3.seconds
    }
  ): fs2.Stream[F, Unit] =
    for {
      subscriber <- fs2.Stream.resource(messageBus.subscribeAwait)
      oldMessages = messageRepo.inFlight
      newMessages = subscriber.through(GameService(Async[F].delay(MessageId.newId), gameRepo, messageRepo))
      event <- (oldMessages ++ newMessages).evalMap {
        case Delayed(message, delay) => messageBus.publish1(message).delayBy(delayDuration(delay)).start.void
        case message: Message        => messageBus.publish1(message)
      }
    } yield ()

