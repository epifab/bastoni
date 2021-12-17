package bastoni.backend

import bastoni.domain.*
import cats.Monad
import cats.effect.std.Queue
import cats.effect.{Concurrent, Sync}
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import fs2.concurrent.Topic

trait MessageBus[F[_]]:

  def publish1(message: Message): F[Unit]
  def publish(messages: fs2.Stream[F, Message]): fs2.Stream[F, Unit]

  def inbox: fs2.Stream[F, Message]
  def outbox: fs2.Stream[F, Message]

  def run: fs2.Stream[F, Unit]

class MessageBusImpl[F[_]](
  inboxTopic: Topic[F, Message],
  inboxQueue: Queue[F, Message],
  outboxTopic: Topic[F, Message],
  outboxQueue: Queue[F, Message],
  override val run: fs2.Stream[F, Unit]
) extends MessageBus[F] {

  def publish1(message: Message): F[Unit] = message match
    case in: MessageIn   => inboxQueue.offer(message)
    case out: MessageOut => outboxQueue.offer(message)

  def publish(messages: fs2.Stream[F, Message]): fs2.Stream[F, Unit] = messages.evalMap(publish1)

  val outbox: fs2.Stream[F, MessageOut] =
    outboxTopic.subscribe(128).collect {
      case message: MessageOut => message
      case rogueMessage: MessageIn => throw new RuntimeException(s"Unexpected outbox message $rogueMessage")
    }

  val inbox: fs2.Stream[F, MessageIn] =
    inboxTopic.subscribe(128).collect {
      case message: MessageIn => message
      case rogueMessage: MessageOut => throw new RuntimeException(s"Unexpected inbox message $rogueMessage")
    }

}


object MessageBus {

  def inMemory[F[_]: Concurrent]: F[MessageBus[F]] = {
    for {
      inbox <- Topic[F, Message]
      inboxQueue <- Queue.bounded[F, Message](128)
      outbox <- Topic[F, Message]
      outboxQueue <- Queue.bounded[F, Message](128)
      run = outbox.publish(fs2.Stream.fromQueueUnterminated(outboxQueue)).merge(inbox.publish(fs2.Stream.fromQueueUnterminated(inboxQueue)))
    } yield new MessageBusImpl[F](inbox, inboxQueue, outbox, outboxQueue, run)
  }

}
