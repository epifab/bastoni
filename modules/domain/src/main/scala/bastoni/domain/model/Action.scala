package bastoni.domain.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.*

enum Action:
  case PlayCard
  case PlayCardOf(suit: Suit)
  case ShuffleDeck

object Action:
  given Encoder[Action] = Encoder.instance {
    case PlayCard         => Json.obj("type" -> "PlayCard".asJson)
    case PlayCardOf(suit) => Json.obj("type" -> "PlayCardOf".asJson, "suit" -> suit.asJson)
    case ShuffleDeck      => Json.obj("type" -> "ShuffleDeck".asJson)
  }

  given Decoder[Action] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "PlayCard"    => Right(PlayCard)
    case "PlayCardOf"  => obj.downField("suit").as[Suit].map(suit => PlayCardOf(suit))
    case "ShuffleDeck" => Right(ShuffleDeck)
  })