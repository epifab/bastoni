package bastoni.domain.logic.briscola

import bastoni.domain.model.{GameScore, Score, UserId, VisibleCard}
import io.circe.{Codec, DecodingFailure}
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.*

case class BriscolaGameScoreItem(card: VisibleCard, points: Int)

object BriscolaGameScoreItem:
  given Codec[BriscolaGameScoreItem] = deriveCodec

case class BriscolaGameScore(playerIds: List[UserId], items: List[BriscolaGameScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.points + _)
  def generify: GameScore  = GameScore(playerIds, points, items.asJson)

object BriscolaGameScore:
  def apply(gameScore: GameScore): Either[DecodingFailure, BriscolaGameScore] =
    gameScore.details.as[List[BriscolaGameScoreItem]].map { items => BriscolaGameScore(gameScore.playerIds, items) }
