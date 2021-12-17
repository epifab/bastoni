package bastoni.domain.model

import java.util.UUID
import scala.util.{Random, Try}
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

opaque type RoomId = UUID

object RoomId:
  def newId: RoomId = UUID.randomUUID()
  def parse(s: String): Option[RoomId] = Try(UUID.fromString(s)).toOption
  given Encoder[RoomId] = Encoder[String].contramap(_.toString)
  given Decoder[RoomId] = Decoder[String].emap(parse(_).toRight("Not a valid ID"))

case class Room(id: RoomId, seats: List[Option[Player]]):
  lazy val players: List[Player] = seats.collect { case Some(player) => player }
  lazy val indexedSeats = seats.zipWithIndex

  val size: Int = seats.size
  val isFull: Boolean = seats.forall(_.isDefined)
  val isEmpty: Boolean = seats.forall(_.isEmpty)

  def contains(p: Player): Boolean = seatFor(p).isDefined

  def seatFor(p: Player): Option[Int] =
    indexedSeats.collectFirst { case (Some(player), index) if player.id == p.id => index }

  def join(p: Player, random: Random): Either[TableError, Room] =
    if (contains(p)) Left(TableError.DuplicatePlayer) else {
      random
        .shuffle(indexedSeats)
        .collectFirst { case (None, index) => index }
        .fold[Either[TableError, Room]](Left(TableError.FullTable)) { targetIndex =>
          Right(copy(
            seats = indexedSeats.map {
              case (_, index) if index == targetIndex => Some(p)
              case (seat, _) => seat
            }
          ))
        }
    }

  def leave(p: Player): Either[TableError, Room] =
    seatFor(p) match
      case Some(targetIndex) =>
        Right(copy(
          seats = indexedSeats.map {
            case (_, index) if index == targetIndex => None
            case (seat, _) => seat
          }
        ))

      case None => Left(TableError.PlayerNotFound)


object Room:
  given Codec[Room] = deriveCodec

  def apply(id: RoomId, seats: Int): Room = Room(id, List.fill(seats)(None))
  def cosy(id: RoomId, players: Player*): Room = Room(id, players.view.map(Some(_)).toList)
