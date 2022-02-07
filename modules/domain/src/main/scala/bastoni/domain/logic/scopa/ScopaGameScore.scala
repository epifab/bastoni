package bastoni.domain.logic.scopa

import bastoni.domain.model.{GameScore, UserId, Score, VisibleCard}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

sealed trait ScopaGameScoreItem(val points: Int)

object ScopaGameScoreItem:
  case class  Carte(count: Int) extends ScopaGameScoreItem(1)
  case class  Denari(count: Int) extends ScopaGameScoreItem(1)
  case object SetteBello extends ScopaGameScoreItem(1)
  case class  Primiera(cards: List[VisibleCard], count: Int) extends ScopaGameScoreItem(1)
  case class  Scope(count: Int) extends ScopaGameScoreItem(count)

  given Encoder[ScopaGameScoreItem] = Encoder.instance {
    case obj: Carte => deriveEncoder[Carte].mapJsonObject(_.add("type", "Carte".asJson))(obj)
    case obj: Denari => deriveEncoder[Denari].mapJsonObject(_.add("type", "Denari".asJson))(obj)
    case SetteBello => Json.obj("type" -> "SetteBello".asJson)
    case obj: Primiera => deriveEncoder[Primiera].mapJsonObject(_.add("type", "Primiera".asJson))(obj)
    case obj: Scope => deriveEncoder[Scope].mapJsonObject(_.add("type", "Scope".asJson))(obj)
  }

  given Decoder[ScopaGameScoreItem] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "Carte" => deriveDecoder[Carte].tryDecode(obj)
    case "Denari" => deriveDecoder[Denari].tryDecode(obj)
    case "SetteBello" => Right(SetteBello)
    case "Primiera" => deriveDecoder[Primiera].tryDecode(obj)
    case "Scope" => deriveDecoder[Scope].tryDecode(obj)
  })

case class ScopaGameScore(playerIds: List[UserId], items: List[ScopaGameScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.points + _)
  def generify: GameScore = GameScore(playerIds, points, items.asJson)

object ScopaGameScore:
  def apply(gameScore: GameScore): Either[DecodingFailure, ScopaGameScore] =
    gameScore.details.as[List[ScopaGameScoreItem]].map(items => ScopaGameScore(gameScore.playerIds, items))
