package bastoni.domain.logic

import cats.effect.Resource

type ServiceRunner[F[_]] = fs2.Stream[F, Unit]
