package bastoni.domain.repos

import bastoni.domain.model.{Delayed, Message, MessageId}
import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*

trait MessageRepo[F[_]]:
  def flying(message: Message | Delayed[Message]): F[Unit]
  def landed(messageId: MessageId): F[Unit]
  def inFlight: fs2.Stream[F, Message | Delayed[Message]]

object MessageRepo:
  private class InMemoryMessageRepo[F[_]](data: Ref[F, Map[MessageId, Message | Delayed[Message]]])
      extends MessageRepo[F]:
    override def flying(message: Message | Delayed[Message]): F[Unit] =
      data.update(_ + (message match
        case m @ Message(id, _, _)             => id -> m
        case d @ Delayed(Message(id, _, _), _) => id -> d
      ))

    override def inFlight: fs2.Stream[F, Message | Delayed[Message]] =
      for
        map     <- fs2.Stream.eval(data.get)
        message <- fs2.Stream.iterable(map.values)
      yield message

    override def landed(messageId: MessageId): F[Unit] = data.update(_ - messageId)

  def inMemory[F[_]: Concurrent]: F[MessageRepo[F]] =
    Ref.of[F, Map[MessageId, Message | Delayed[Message]]](Map.empty).map(new InMemoryMessageRepo(_))
