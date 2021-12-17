package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import cats.Monad
import cats.effect.Sync
import cats.syntax.all.*

import scala.util.Random

object Lobby:
  def apply[F[_]: Monad](roomSize: Int, newId: F[MessageId], seed: F[Int])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .evalScan[F, (Map[RoomId, Room], Option[Message])](Map.empty -> None) {
        case ((lobby, _), Message(_, roomId, JoinRoom(player))) =>
          for {
            s <- seed
            id <- newId
            results = lobby
              .getOrElse(roomId, Room(roomId, roomSize))
              .join(player, Random(s))
              .fold(
                _ => lobby -> None,
                { newRoom =>
                  val newLobby = lobby + (roomId -> newRoom)
                  newLobby -> Some(Message(id, roomId, PlayerJoined(player, newRoom)))
                }
              )
          } yield results

        case ((lobby, _), Message(_, roomId, LeaveRoom(player))) =>
          for {
            id <- newId
            results = lobby
              .getOrElse(roomId, Room(roomId, roomSize))
              .leave(player)
              .fold(
                _ => lobby -> None,
                { newRoom =>
                  val newLobby = if (newRoom.isEmpty) lobby - roomId else lobby + (roomId -> newRoom)
                  newLobby -> Some(Message(id, roomId, PlayerLeft(player, newRoom)))
                }
              )
          } yield results

        case ((lobby, _), Message(_, roomId, ActivateRoom(player, gameType))) =>
          for {
            id <- newId
            done = lobby.getOrElse(roomId, Room(roomId, roomSize)) match
              case room if room.contains(player) && room.players.size > 1 =>
                lobby -> Some(Message(id, roomId, StartGame(room, gameType)))
              case _ => lobby -> None
          } yield done

        case ((lobby, _), _) => Monad[F].pure(lobby -> None)
      }
      .collect { case (_, Some(message)) => message }

  def run[F[_]: Sync](messageBus: MessageBus[F]): fs2.Stream[F, Unit] =
    messageBus
      .subscribe
      .through(apply(4, Sync[F].delay(MessageId.newId), Sync[F].delay(Random.nextInt())))
      .through(messageBus.publish)
