package bastoni.domain

case class Message(val roomId: RoomId, val message: Command | Event)
