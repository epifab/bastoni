package bastoni.domain.logic

import cats.effect.Resource

type ServiceRunner[F[_]] = Resource[F, fs2.Stream[F, Unit]]
