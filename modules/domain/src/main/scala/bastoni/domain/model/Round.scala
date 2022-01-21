package bastoni.domain.model

type Round[T] = List[T]

extension[T](xs: Round[T])
  def slideUntil(f: T => Boolean): Round[T] =
    (LazyList.from(xs) ++ LazyList.from(xs))
      .dropWhile(!f(_))
      .take(xs.size)
      .toList

  def shift: Round[T] = xs.drop(1) ++ xs.headOption.toList
  def shiftBackwards: Round[T] = xs.lastOption.fold(xs)(last => last :: xs.init)
