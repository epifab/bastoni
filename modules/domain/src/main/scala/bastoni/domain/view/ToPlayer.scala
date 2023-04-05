package bastoni.domain.view

import bastoni.domain.model.{PlayerEvent, RoomPlayerView}
import bastoni.domain.model.Command.Act
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

sealed trait ToPlayer

object ToPlayer:
  case class Snapshot(room: RoomPlayerView) extends ToPlayer
  case class GameEvent(event: PlayerEvent)  extends ToPlayer
  case class Request(act: Act)              extends ToPlayer
  case object Ping                          extends ToPlayer

  given Encoder[ToPlayer] = ConfiguredEncoder.derive(discriminator = Some("messageType"))
  given Decoder[ToPlayer] = ConfiguredDecoder.derive(discriminator = Some("messageType"))
