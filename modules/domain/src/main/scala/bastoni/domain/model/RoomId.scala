package bastoni.domain.model

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

import java.util.UUID
import scala.util.{Random, Try}

opaque type RoomId = UUID

object RoomId:
  def newId: RoomId                       = UUID.randomUUID()
  def apply(id: UUID): RoomId             = id
  def tryParse(s: String): Option[RoomId] = Try(unsafeParse(s)).toOption
  def unsafeParse(s: String): RoomId      = UUID.fromString(s)

  extension (roomId: RoomId) def value: String = roomId.toString

  given Encoder[RoomId] = Encoder.encodeUUID
  given Decoder[RoomId] = Decoder.decodeUUID
