package bastoni.domain.logic

import bastoni.domain.model.Message
import cats.effect.{Concurrent, Resource}
import cats.effect.std.Queue
import cats.syntax.all.*

trait MessageQueue[F[_]] extends MessageConsumer[F]:
  def run: fs2.Stream[F, Unit]

object MessageQueue:
  def inMemory[F[_]: Concurrent](bus: MessageBus[F]): Resource[F, MessageQueue[F]] =
    for
      queue  <- Resource.eval(Queue.bounded[F, Message](128))
      stream <- bus.subscribe
    yield new MessageQueue[F]:

      override val run: fs2.Stream[F, Unit] =
        stream.evalMap(queue.offer)

      override val consume: fs2.Stream[F, Message] =
        fs2.Stream.fromQueueUnterminated(queue)
