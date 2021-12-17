package bastoni.backend

import bastoni.domain.model.MessageId

trait MessagesRepo[F[_]]:
  def messageIds: fs2.Stream[F, MessageId]
