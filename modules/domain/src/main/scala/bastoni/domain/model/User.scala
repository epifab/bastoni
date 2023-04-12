package bastoni.domain.model

import io.circe.{Codec, Decoder, Encoder, KeyDecoder, KeyEncoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

import java.util.UUID
import scala.util.Try

opaque type UserId = UUID

object UserId:
  def newId: UserId                       = UUID.randomUUID()
  def tryParse(s: String): Option[UserId] = Try(unsafeParse(s)).toOption
  def unsafeParse(s: String): UserId      = UUID.fromString(s)

  given Encoder[UserId]    = Encoder[String].contramap(_.toString)
  given Decoder[UserId]    = Decoder[String].emap(tryParse(_).toRight("Not a valid ID"))
  given KeyEncoder[UserId] = KeyEncoder.encodeKeyUUID
  given KeyDecoder[UserId] = KeyDecoder.decodeKeyUUID

  extension (userId: UserId) def value: String = userId.toString

trait User(val id: UserId, val name: String):
  def is(other: User): Boolean   = other.id == id
  def is(other: UserId): Boolean = other == id

case class BaseUser(override val id: UserId, override val name: String) extends User(id, name)

object User:
  def apply(id: UserId, name: String): User = BaseUser(id, name)
  given Encoder[User] = deriveEncoder[BaseUser].contramap[User](user => BaseUser(user.id, user.name))
  given Decoder[User] = deriveDecoder[BaseUser].map[User](identity)
