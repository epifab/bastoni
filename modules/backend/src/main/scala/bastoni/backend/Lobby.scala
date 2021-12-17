package bastoni.backend

import bastoni.domain.*

object Lobby:

  extension (room: Room)
    def contains(p: Player): Boolean = room.players.exists(_ == p)

    def join(p: Player): Room =
      room.copy(players = p :: room.players)

    def leave(p: Player): Room =
      room.copy(players = room.players.filterNot(_ == p))

    def withEvent(f: Room => Event): (Room, Option[Event]) =
      room -> Some(f(room))

  def apply[F[_]](roomId: RoomId, roomSize: Int, messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .collect { case MessageIn(`roomId`, command: Command) => command }
      .scan[(Room, Option[Event])](Room(roomId, Nil) -> None) {
        case ((room, _), JoinRoom(player)) if room.players.size < roomSize => room.join(player).withEvent(PlayerJoined(player, _))
        case ((room, _), LeaveRoom(player)) if room.contains(player) => room.leave(player).withEvent(PlayerLeft(player, _))
        case ((room, _), _) => room -> None
      }
      .collect { case (room, Some(e)) => Message(roomId, e) }
