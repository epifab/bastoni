package bastoni.domain.logic.tressette

import bastoni.domain.model.{GameScore, Score, UserId, VisibleCard}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

sealed trait TressetteGameScoreItem:
  def thirds: 1 | 3

object TressetteGameScoreItem:
  case class Carta(card: VisibleCard, thirds: 1 | 3) extends TressetteGameScoreItem

  case object Rete extends TressetteGameScoreItem:
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

  given Encoder[TressetteGameScoreItem] = Encoder.instance {
    case obj: Carta => deriveEncoder[Carta].mapJsonObject(_.add("type", "Carta".asJson))(obj)
    case Rete => Json.obj("type" -> "Rete".asJson)
  }

  given Decoder[TressetteGameScoreItem] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "Carta" => deriveDecoder[Carta].tryDecode(obj)
    case "Rete" => Right(Rete)
  })

case class TressetteGameScore(playerIds: List[UserId], items: List[TressetteGameScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.thirds + _) / 3
  def generify: GameScore = GameScore(playerIds, points, items.asJson)

object TressetteGameScore:
  def apply(gameScore: GameScore): Either[DecodingFailure, TressetteGameScore] =
    gameScore.details.as[List[TressetteGameScoreItem]].map(items => TressetteGameScore(gameScore.playerIds, items))
