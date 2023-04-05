package bastoni.domain.logic

import bastoni.domain.model.*
import cats.effect.{Concurrent, Resource, Sync}
import cats.effect.std.Queue
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import cats.Monad
import fs2.concurrent.Topic

trait Bus[F[_], A] extends Subscriber[F, A], Publisher[F, A]:
  def run: fs2.Stream[F, Unit]
  def subscribe: Resource[F, fs2.Stream[F, A]]

object InMemoryBus:

  private class InMemoryBus[F[_], A](
      topic: Topic[F, A],
      queue: Queue[F, A],
      val run: fs2.Stream[F, Unit]
  ) extends Bus[F, A]:
    def publish1(message: A): F[Unit] = queue.offer(message)

    def publish(messages: fs2.Stream[F, A]): fs2.Stream[F, Unit] = messages.evalMap(publish1)

    val consume: fs2.Stream[F, A]                = topic.subscribe(128)
    val subscribe: Resource[F, fs2.Stream[F, A]] = topic.subscribeAwait(128)

  def apply[F[_]: Concurrent, A]: F[Bus[F, A]] =
    for
      topic <- Topic[F, A]
      queue <- Queue.bounded[F, A](128)
      run = topic.publish(fs2.Stream.fromQueueUnterminated(queue))
    yield new InMemoryBus[F, A](topic, queue, run)

type MessageBus[F[_]] = Bus[F, Message]

object MessageBus:
  def inMemory[F[_]: Concurrent]: F[MessageBus[F]] = InMemoryBus[F, Message]
