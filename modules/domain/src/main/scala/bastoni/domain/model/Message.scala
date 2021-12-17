package bastoni.domain.model

import java.util.UUID
import scala.util.Try

opaque type MessageId = UUID

object MessageId:
  def newId: MessageId = UUID.randomUUID()
  def parse(s: String): Option[MessageId] = Try(UUID.fromString(s)).toOption

case class Message(messageId: MessageId, roomId: RoomId, message: Command | Event)
