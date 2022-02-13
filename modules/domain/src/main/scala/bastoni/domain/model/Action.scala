package bastoni.domain.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.*

sealed trait PlayContext(val gameType: GameType)

object PlayContext:
  case class  Briscola(trump: Suit) extends PlayContext(GameType.Briscola)
  case class  Tressette(trump: Option[Suit]) extends PlayContext(GameType.Tressette)
  case object Scopa extends PlayContext(GameType.Scopa)

  given Encoder[PlayContext] = Encoder.instance {
    case c@ Briscola(trump) => Json.obj("type" -> c.gameType.asJson, "trump" -> trump.asJson)
    case c@ Tressette(trump) => Json.obj("type" -> c.gameType.asJson, "trump" -> trump.asJson)
    case c@ Scopa => Json.obj("type" -> c.gameType.asJson)
  }

  given Decoder[PlayContext] = Decoder.instance(cursor => cursor.downField("type").as[GameType].flatMap {
    case GameType.Briscola => cursor.as[Briscola](using deriveDecoder)
    case GameType.Tressette => cursor.as[Tressette](using deriveDecoder)
    case GameType.Scopa => Right(Scopa)
  })


enum Action:
  case PlayCard(context: PlayContext)
  case ShuffleDeck
  case Confirm

object Action:
  given Encoder[Action] = Encoder.instance {
    case PlayCard(context) => Json.obj("type" -> "PlayCard".asJson, "context" -> context.asJson)
    case ShuffleDeck       => Json.obj("type" -> "ShuffleDeck".asJson)
    case Confirm           => Json.obj("type" -> "Confirm".asJson)
  }

  given Decoder[Action] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "PlayCard"    => obj.downField("context").as[PlayContext].map(PlayCard(_))
    case "ShuffleDeck" => Right(ShuffleDeck)
    case "Confirm"     => Right(Confirm)
  })
