package bastoni.domain.logic

import bastoni.domain.model.*
import cats.Monad
import cats.effect.std.Queue
import cats.effect.{Resource, Concurrent, Sync}
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import fs2.concurrent.Topic


trait Bus[F[_], A]:
  def publish1(message: A): F[Unit]
  def publish(messages: fs2.Stream[F, A]): fs2.Stream[F, Unit]
  def subscribe: fs2.Stream[F, A]
  def subscribeAwait: Resource[F, fs2.Stream[F, A]]
  def run: fs2.Stream[F, Unit]


class Fs2Bus[F[_], A] private(
  topic: Topic[F, A],
  queue: Queue[F, A],
  override val run: fs2.Stream[F, Unit]
) extends Bus[F, A]:
  def publish1(message: A): F[Unit] = queue.offer(message)
  def publish(messages: fs2.Stream[F, A]): fs2.Stream[F, Unit] = messages.evalMap(publish1)
  val subscribe: fs2.Stream[F, A] = topic.subscribe(128)
  val subscribeAwait: Resource[F, fs2.Stream[F, A]] = topic.subscribeAwait(128)

object Fs2Bus:
  def apply[F[_]: Concurrent, A]: F[Bus[F, A]] =
    for {
      topic <- Topic[F, A]
      queue <- Queue.bounded[F, A](128)
      run = topic.publish(fs2.Stream.fromQueueUnterminated(queue))
    } yield new Fs2Bus[F, A](topic, queue, run)


type MessageBus[F[_]] = Bus[F, Message]

object MessageBus:
  def inMemory[F[_]: Concurrent]: F[MessageBus[F]] = Fs2Bus[F, Message]
