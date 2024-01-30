package bastoni.domain.view

import bastoni.domain.model.{PlayerEvent, RoomPlayerView, User}
import bastoni.domain.model.Command.Act
import cats.Show
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}

sealed trait ConnectionError

object ConnectionError:
  case object InvalidToken extends RuntimeException("Invalid auth token") with ConnectionError
  case object Forbidden    extends RuntimeException("A connection message was expected") with ConnectionError

  given Encoder[ConnectionError] = Encoder.encodeString.contramap {
    case InvalidToken => "invalid-token"
    case Forbidden    => "forbidden"
  }

  given Decoder[ConnectionError] = Decoder.decodeString.map {
    case "invalid-token" => InvalidToken
    case _               => Forbidden
  }

sealed trait ToPlayer

object ToPlayer:
  case class Authenticated(user: User)             extends ToPlayer
  case class Connected(room: RoomPlayerView)       extends ToPlayer
  case class Disconnected(reason: ConnectionError) extends ToPlayer
  case class GameEvent(event: PlayerEvent)         extends ToPlayer
  case class Request(act: Act)                     extends ToPlayer
  case object Ping                                 extends ToPlayer

  given Encoder[ToPlayer] = ConfiguredEncoder.derive(discriminator = Some("messageType"))
  given Decoder[ToPlayer] = ConfiguredDecoder.derive(discriminator = Some("messageType"))

  given Show[ToPlayer] = Show(Encoder[ToPlayer].apply(_).spaces2)
