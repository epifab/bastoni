package bastoni.domain.model

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

import java.util.UUID
import scala.util.{Random, Try}

opaque type RoomId = UUID

object RoomId:
  def newId: RoomId                       = UUID.randomUUID()
  def tryParse(s: String): Option[RoomId] = Try(unsafeParse(s)).toOption
  def unsafeParse(s: String): RoomId      = UUID.fromString(s)
  given Encoder[RoomId]                   = Encoder[String].contramap(_.toString)
  given Decoder[RoomId]                   = Decoder[String].emap(tryParse(_).toRight("Not a valid ID"))
