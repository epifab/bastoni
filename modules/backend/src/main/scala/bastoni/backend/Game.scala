package bastoni.backend

import bastoni.domain.{Message, Room}

trait Game:
  def playMatch[F[_]](room: Room)(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message]
