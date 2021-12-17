package bastoni.domain.model

type Round[T] = List[T]

extension[T](xs: Round[T])
  def slideUntil(f: T => Boolean): Round[T] =
    (LazyList.from(xs) ++ LazyList.from(xs))
      .dropWhile(!f(_))
      .take(xs.size)
      .toList
