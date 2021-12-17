package bastoni.domain.logic.tressette

import bastoni.domain.model.*
import bastoni.domain.model.Rank.*
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.syntax.*

sealed trait GameScoreItem:
  def thirds: 1 | 3

object GameScoreItem:
  case class Carta(card: Card, thirds: 1 | 3) extends GameScoreItem
  case object Rete extends GameScoreItem:
    override val thirds = 3

  given Encoder[1 | 3] = Encoder.encodeInt.contramap {
    case 1 => 1
    case 3 => 3
  }
  given Decoder[1 | 3] = Decoder.decodeInt.emap {
    case 1 => Right(1)
    case 3 => Right(3)
    case x => Left(s"Invalid value $x")
  }

  given Encoder[GameScoreItem] = Encoder.instance {
    case obj: Carta => deriveEncoder[Carta].mapJsonObject(_.add("type", "Carta".asJson))(obj)
    case Rete => Json.obj("type" -> "Rete".asJson)
  }

  given Decoder[GameScoreItem] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "Carta" => deriveDecoder[Carta].tryDecode(obj)
    case "Rete" => Right(Rete)
  })

case class GameScore(playerIds: List[UserId], items: List[GameScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.thirds + _) / 3

object GameScore:
  def apply(players: List[Player], rete: Boolean): GameScore =
    new GameScore(
      players.map(_.id),
      Option.when(rete)(GameScoreItem.Rete).toList ++ players.flatMap(_.taken).collect {
        case card@ Card(Asso, _) => GameScoreItem.Carta(card, 3)
        case card@ Card(Tre, _) => GameScoreItem.Carta(card, 1)
        case card@ Card(Due, _) => GameScoreItem.Carta(card, 1)
        case card@ Card(Re, _) => GameScoreItem.Carta(card, 1)
        case card@ Card(Cavallo, _) => GameScoreItem.Carta(card, 1)
        case card@ Card(Fante, _) => GameScoreItem.Carta(card, 1)
      }
    )

  given Codec[GameScore] = deriveCodec
