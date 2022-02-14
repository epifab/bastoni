package bastoni.domain.logic.briscola

import bastoni.domain.model.Rank.{Asso, Cavallo, Fante, Re, Tre}
import bastoni.domain.model.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

object BriscolaGameScoreCalculator:
  def pointsFor(card: Card): Int = card.rank match {
    case Rank.Asso => 11
    case Rank.Tre => 10
    case Rank.Re => 4
    case Rank.Cavallo => 3
    case Rank.Fante => 2
    case _ => 0
  }

  def apply(players: List[Player]): BriscolaGameScore =
    BriscolaGameScore(
      players.map(_.id),
      players
        .flatMap(_.taken)
        .map(card => card -> pointsFor(card))
        .collect { case (card, points) if points > 0 => BriscolaGameScoreItem(card, points) }
        .sortBy(-_.points)
    )
