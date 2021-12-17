package bastoni.domain

import java.util.UUID
import scala.util.Try

opaque type RoomId = UUID

object RoomId:
  def newId: RoomId = UUID.randomUUID()
  def parse(s: String): Option[RoomId] = Try(UUID.fromString(s)).toOption

case class Room(id: RoomId, players: List[Player])
