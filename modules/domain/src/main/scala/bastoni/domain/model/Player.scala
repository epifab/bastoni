package bastoni.domain.model

import java.util.UUID
import scala.util.Try

import io.circe.{Codec, Encoder, Decoder}
import io.circe.generic.semiauto.deriveCodec

opaque type PlayerId = UUID

object PlayerId:
  def newId: PlayerId = UUID.randomUUID()
  def tryParse(s: String): Option[PlayerId] = Try(unsafeParse(s)).toOption
  def unsafeParse(s: String): PlayerId = UUID.fromString(s)
  given Encoder[PlayerId] = Encoder[String].contramap(_.toString)
  given Decoder[PlayerId] = Decoder[String].emap(tryParse(_).toRight("Not a valid ID"))

case class Player(id: PlayerId, name: String)

object Player:
  given Codec[Player] = deriveCodec
