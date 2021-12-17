package bastoni.domain

sealed abstract class Message(val roomId: RoomId, val message: Command | Event)

object Message:
  def apply(roomId: RoomId, message: Command | Event): Message =
    message match
      case command: Command => MessageIn(roomId, command)
      case event: Event => MessageOut(roomId, event)

  def unapply(message: Message): (RoomId, Command | Event) =
    message.roomId -> message.message

case class MessageIn(override val roomId: RoomId, command: Command) extends Message(roomId, command)
case class MessageOut(override val roomId: RoomId, event: Event) extends Message(roomId, event)
