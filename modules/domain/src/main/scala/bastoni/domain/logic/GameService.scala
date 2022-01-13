package bastoni.domain.logic

import bastoni.domain.logic.briscola
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.repos.{GameRepo, MessageRepo}
import cats.Applicative
import cats.effect.syntax.all.*
import cats.effect.{Async, Concurrent, Resource}
import cats.syntax.all.*
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import scala.concurrent.duration.*

extension (data: List[Command | Delayed[Command] | ServerEvent])
  def toMessages[F[_]: Applicative](roomId: RoomId, newId: F[MessageId]): F[List[Message | Delayed[Message]]] =
    data
      .traverse {
        case event: ServerEvent               => newId.map(id => Message(id, roomId, event))
        case command: Command                 => newId.map(id => Message(id, roomId, command))
        case Delayed(command: Command, delay) => newId.map(id => Delayed(Message(id, roomId, command), delay))
      }

object GameService:

  def apply[F[_]: Concurrent](newId: F[MessageId], gameRepo: GameRepo[F], messageRepo: MessageRepo[F])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    messages
      .evalMap { case Message(id, roomId, data) =>
        for {
          context <- gameRepo.get(roomId)
          (newContext, messagesData) = context.getOrElse(GameContext.build(4)).apply(data)
          messages <- messagesData.toMessages(roomId, newId)
          // the remaining operations should be done atomically to guarantee consistency
          _ <- messageRepo.landed(id)
          _ <- messages.traverse(messageRepo.flying)
          _ <- newContext.fold(gameRepo.remove(roomId))(gameRepo.set(roomId, _))
        } yield messages
      }
      .flatMap(fs2.Stream.iterable)

  def runner[F[_]: Async](
    messageBus: MessageBus[F],
    gameRepo: GameRepo[F],
    messageRepo: MessageRepo[F],
    delayDuration: Delay => FiniteDuration = {
      case Delay.DealCards => 700.millis
      case Delay.TakeCards => 1.second
      case Delay.CompleteGame => 3.seconds
      case Delay.ActionTimeout => 3.seconds // players get 10 * 3 = 30 seconds to act
    }
  ): ServiceRunner[F] =
    messageBus.subscribeAwait.map { subscription =>
      val oldMessages = messageRepo.inFlight
      val newMessages = subscription.through(GameService(Async[F].delay(MessageId.newId), gameRepo, messageRepo))

      (oldMessages ++ newMessages).evalMap {
        case Delayed(message, delay) => messageBus.publish1(message).delayBy(delayDuration(delay)).start.void
        case message: Message        => messageBus.publish1(message)
      }
    }
