package bastoni.backend

import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
import cats.effect.Sync

object Lobby:

  extension (room: Room)
    def contains(p: Player): Boolean = room.players.exists(_ == p)

    def join(p: Player): Room =
      room.copy(players = p :: room.players)

    def leave(p: Player): Room =
      room.copy(players = room.players.filterNot(_ == p))

  def apply[F[_]](roomMaxSize: Int, messageIds: fs2.Stream[F, MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .scan[(Map[RoomId, Room], Option[(RoomId, Event | Command)])](Map.empty -> None) {
        case ((lobby, _), Message(_, roomId, JoinRoom(player))) =>
          lobby.getOrElse(roomId, Room(roomId, Nil)) match
            case room if room.players.size < roomMaxSize && !room.contains(player) =>
              val newRoom = room.join(player)
              (lobby + (roomId -> newRoom)) -> Some(roomId -> PlayerJoined(player, newRoom))
            case _ => lobby -> None

        case ((lobby, _), Message(_, roomId, LeaveRoom(player))) =>
          lobby.getOrElse(roomId, Room(roomId, Nil)) match
            case room if room.contains(player) =>
              val newRoom = room.leave(player)
              val newLobby = if (newRoom.players.isEmpty) lobby - roomId else lobby + (roomId -> newRoom)
              newLobby -> Some(roomId -> PlayerLeft(player, newRoom))
            case _ => lobby -> None

        case ((lobby, _), Message(_, roomId, ActivateRoom(player, gameType))) =>
          lobby.getOrElse(roomId, Room(roomId, Nil)) match
            case room if room.contains(player) && room.players.size > 1 =>
              lobby -> Some(roomId -> StartGame(room, gameType))
            case _ => lobby -> None

        case ((lobby, _), _) => lobby -> None
      }
      .collect { case (room, Some((roomId, event))) => (roomId, event) }
      .zip(messageIds)
      .map { case ((roomId, event), id) => Message(id, roomId, event) }

  def run[F[_]: Sync](messageBus: MessageBus[F]): fs2.Stream[F, Unit] =
    messageBus
      .subscribe
      .through(apply(4, fs2.Stream.repeatEval(Sync[F].delay(MessageId.newId))))
      .through(messageBus.publish)
