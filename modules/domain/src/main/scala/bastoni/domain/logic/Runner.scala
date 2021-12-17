package bastoni.domain.logic

import cats.effect.Resource

type Runner[F[_]] = Resource[F, fs2.Stream[F, Unit]]
