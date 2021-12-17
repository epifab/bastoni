package bastoni.backend

import bastoni.domain.*

case object FullRoom
case object PlayerNotFound

extension (room: Room)
  def contains(p: Player): Boolean = room.players.exists(_ == p)

  def isFull: Boolean = room.players.size == room.size
  def isEmpty: Boolean = room.players.isEmpty

  def join(p: Player): Either[FullRoom.type, Room] =
    Either.cond(isFull, room.copy(players = p :: room.players), FullRoom)

  def leave(p: Player): Either[PlayerNotFound.type, Room] =
    Either.cond(contains(p), room.copy(players = room.players.filterNot(_ == p)), PlayerNotFound)


object Lobby:
  def apply[F[_]](roomId: RoomId, roomSize: Int, messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .collect { case Message(`roomId`, command: Command) => command }
      .fold[(Room, Option[Event])](Room(roomId, Nil, roomSize) -> None) {
        case ((room, _), JoinRoom(player)) =>
          room.join(player)
            .map(newRoom => newRoom -> Some(PlayerJoined(player, newRoom)))
            .getOrElse(room -> None)
        case ((room, _), LeaveRoom(player)) =>
          room.leave(player)
            .map(newRoom => newRoom -> Some(PlayerLeft(player, newRoom)))
            .getOrElse(room -> None)
        case ((room, _), _) => room -> None
      }
      .collect { case (room, Some(e)) => Message(roomId, e) }
