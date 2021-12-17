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
  def subscribe: fs2.Stream[F, Message]
  def run: fs2.Stream[F, Unit]

class MessageBusImpl[F[_]](
  topic: Topic[F, Message],
  queue: Queue[F, Message],
  override val run: fs2.Stream[F, Unit]
) extends MessageBus[F]:
  def publish1(message: Message): F[Unit] = queue.offer(message)
  def publish(messages: fs2.Stream[F, Message]): fs2.Stream[F, Unit] = messages.evalMap(publish1)
  val subscribe: fs2.Stream[F, Message] = topic.subscribe(128)


object MessageBus {

  def inMemory[F[_]: Concurrent]: F[MessageBus[F]] = {
    for {
      topic <- Topic[F, Message]
      queue <- Queue.bounded[F, Message](128)
      run = topic.publish(fs2.Stream.fromQueueUnterminated(queue))
    } yield new MessageBusImpl[F](topic, queue, run)
  }

}
