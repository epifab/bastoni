package bastoni.domain

import java.util.UUID
import scala.util.Try

opaque type PlayerId = UUID

object PlayerId:
  def newId: PlayerId = UUID.randomUUID()
  def parse(s: String): Option[PlayerId] = Try(UUID.fromString(s)).toOption

case class Player(id: PlayerId, name: String)
