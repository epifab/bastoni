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
