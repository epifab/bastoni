package bastoni.domain.logic

import bastoni.domain.model.Message
import cats.effect.Resource

trait Consumer[F[_], A]:
  def consume: fs2.Stream[F, A]

type MessageConsumer[F[_]] = Consumer[F, Message]
