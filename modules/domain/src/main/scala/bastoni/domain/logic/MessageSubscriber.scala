package bastoni.domain.logic

import bastoni.domain.model.Message
import cats.effect.Resource

trait Subscriber[F[_], A]:
  def subscribe: Resource[F, fs2.Stream[F, A]]

type MessageSubscriber[F[_]] = Subscriber[F, Message]
