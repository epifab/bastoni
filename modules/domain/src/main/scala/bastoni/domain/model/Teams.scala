package bastoni.domain.model

object Teams:
  def apply[T <: User](players: List[T]): List[List[T]] =
    players match {
      case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
      case _ => players.map(List(_))
    }
