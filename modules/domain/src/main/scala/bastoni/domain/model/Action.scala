package bastoni.domain.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.*

sealed trait PlayContext(val gameType: GameType)

object PlayContext:
  case class Briscola(trump: Suit)          extends PlayContext(GameType.Briscola)
  case class Tressette(trump: Option[Suit]) extends PlayContext(GameType.Tressette)
  case object Scopa                         extends PlayContext(GameType.Scopa)

  given Encoder[PlayContext] = Encoder.instance {
    case c @ Briscola(trump)  => Json.obj("type" -> c.gameType.asJson, "trump" -> trump.asJson)
    case c @ Tressette(trump) => Json.obj("type" -> c.gameType.asJson, "trump" -> trump.asJson)
    case c @ Scopa            => Json.obj("type" -> c.gameType.asJson)
  }

  given Decoder[PlayContext] = Decoder.instance(cursor =>
    cursor.downField("type").as[GameType].flatMap {
      case GameType.Briscola  => cursor.as[Briscola](using deriveDecoder)
      case GameType.Tressette => cursor.as[Tressette](using deriveDecoder)
      case GameType.Scopa     => Right(Scopa)
    }
  )

enum Action:
  case PlayCard(context: PlayContext)
  case ShuffleDeck
  case Confirm

object Action:
  given Encoder[Action] = ConfiguredEncoder.derive(discriminator = Some("type"))
  given Decoder[Action] = ConfiguredDecoder.derive(discriminator = Some("type"))
