package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.repos.RoomRepo
import cats.Monad
import cats.effect.Sync
import cats.syntax.all.*

import scala.util.Random

object Lobby:
  def apply[F[_]: Monad](roomSize: Int, newId: F[MessageId], seed: F[Int], repo: RoomRepo[F])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .evalMap {
        case Message(_, roomId, JoinRoom(player)) =>
          for {
            s <- seed
            id <- newId
            room <- repo.get(roomId).map(_.getOrElse(Room(roomId, roomSize)))
            result = room.join(player, Random(s))
            _ <- result.traverse(newRoom => repo.set(roomId, newRoom))
            event = result.toOption.map(newRoom => Message(id, roomId, PlayerJoined(player, newRoom)))
          } yield event

        case Message(_, roomId, LeaveRoom(player)) =>
          for {
            id <- newId
            room <- repo.get(roomId).map(_.getOrElse(Room(roomId, roomSize)))
            result = room.leave(player)
            _ <- result.traverse {
              case emptyRoom if emptyRoom.isEmpty => repo.remove(roomId)
              case newRoom => repo.set(roomId, newRoom)
            }
            event = result.toOption.map(newRoom => Message(id, roomId, PlayerLeft(player, newRoom)))
          } yield event

        case Message(_, roomId, ActivateRoom(player, gameType)) =>
          for {
            id <- newId
            room <- repo.get(roomId).map(_.getOrElse(Room(roomId, roomSize)))
            event: Option[Message] = Option.when(room.contains(player) && room.players.size > 1)(Message(id, roomId, StartGame(room, gameType)))
          } yield event

        case _ => Monad[F].pure(None)
      }
      .collect { case Some(message) => message }

  def run[F[_]: Sync](messageBus: MessageBus[F], repo: RoomRepo[F]): fs2.Stream[F, Unit] =
    messageBus
      .subscribe
      .through(Lobby(roomSize = 4, Sync[F].delay(MessageId.newId), Sync[F].delay(Random.nextInt()), repo))
      .through(messageBus.publish)
