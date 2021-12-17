package bastoni.domain.model

case class Message(val roomId: RoomId, val message: Command | Event)
