package bastoni.domain.logic.briscola

import bastoni.domain.model.Rank.{Asso, Cavallo, Fante, Re, Tre}
import bastoni.domain.model.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

case class GameScoreItem(card: Card, points: Int)

case class GameScore(playerIds: List[UserId], items: List[GameScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.points + _)

object GameScore:
  def pointsFor(card: Card): Int = card.rank match {
    case Rank.Asso => 11
    case Rank.Tre => 10
    case Rank.Re => 4
    case Rank.Cavallo => 3
    case Rank.Fante => 2
    case _ => 0
  }

  def apply(players: List[Player]): GameScore =
    new GameScore(
      players.map(_.id),
      players.flatMap(_.taken).map(card => card -> pointsFor(card)).collect {
        case (card, points) if points > 0 => GameScoreItem(card, points)
      }
    )

  given Codec[GameScoreItem] = deriveCodec
  given Codec[GameScore] = deriveCodec
