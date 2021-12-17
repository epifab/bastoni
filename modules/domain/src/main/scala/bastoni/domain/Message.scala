package bastoni.domain

case class Message(roomId: RoomId, message: Command | Event)
