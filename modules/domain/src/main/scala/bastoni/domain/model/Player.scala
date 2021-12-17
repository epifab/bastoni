package bastoni.domain.model

import java.util.UUID
import scala.util.Try
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

opaque type PlayerId = UUID

object PlayerId:
  def newId: PlayerId = UUID.randomUUID()
  def tryParse(s: String): Option[PlayerId] = Try(unsafeParse(s)).toOption
  def unsafeParse(s: String): PlayerId = UUID.fromString(s)
  given Encoder[PlayerId] = Encoder[String].contramap(_.toString)
  given Decoder[PlayerId] = Decoder[String].emap(tryParse(_).toRight("Not a valid ID"))

trait Player(val id: PlayerId, val name: String):
  def is(other: Player): Boolean = other.id == id
  def is(other: PlayerId): Boolean = other == id

case class BasePlayer(override val id: PlayerId, override val name: String) extends Player(id, name)

object Player:
  def apply(id: PlayerId, name: String): Player = BasePlayer(id, name)
  given Encoder[Player] = deriveEncoder[BasePlayer].contramap[Player](player => BasePlayer(player.id, player.name))
  given Decoder[Player] = deriveDecoder[BasePlayer].map[Player](identity)
