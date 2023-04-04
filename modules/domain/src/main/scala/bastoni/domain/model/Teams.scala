package bastoni.domain.model

object Teams:
  def size[T <: User](players: List[T]): Int = if (players.length == 4) 2 else 1

  def apply[T <: User](players: List[T]): List[List[T]] =
    players match
      case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
      case _                       => players.map(List(_))
