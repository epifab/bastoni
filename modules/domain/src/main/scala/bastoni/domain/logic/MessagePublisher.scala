package bastoni.domain.logic

import bastoni.domain.model.Message

trait Publisher[F[_], A]:
  def publish1(message: A): F[Unit]
  def publish(messages: fs2.Stream[F, A]): fs2.Stream[F, Unit]

type MessagePublisher[F[_]] = Publisher[F, Message]
