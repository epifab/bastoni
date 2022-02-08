package bastoni.domain.logic.scopa

import bastoni.domain.model.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder, Json}


object ScopaGameScoreCalculator:

  def apply(teams: List[List[Player]]): List[ScopaGameScore] =
    val teamWithCards: Map[List[Player], List[VisibleCard]] = teams.map(team => team -> team.flatMap(_.taken)).toMap

    val carte: Option[(List[Player], ScopaGameScoreItem)] =
      teamWithCards
        .view.mapValues(cards => ScopaGameScoreItem.Carte(cards.size))
        .toList.bestBy(_._2.count)

    val denari: Option[(List[Player], ScopaGameScoreItem)] =
      teamWithCards
        .view.mapValues(cards => ScopaGameScoreItem.Denari(cards.count(_.suit == Denari)))
        .toList.bestBy(_._2.count)

    val primiera: Option[(List[Player], ScopaGameScoreItem)] =
      teamWithCards
        .flatMap { case (team, cards) => calculatePrimiera(cards).map(team -> _) }
        .maxByOption(_._2.count)

    val setteBello: Option[(List[Player], ScopaGameScoreItem)] =
      teamWithCards
        .collectFirst { case (team, cards) if cards.exists(c => c.rank == Sette && c.suit == Denari) =>
          team -> ScopaGameScoreItem.SetteBello
        }

    teams.map(team =>
      val points =
        List(carte, denari, primiera, setteBello)
          .flatMap(_.collect { case (t, s) if t == team => s })

      ScopaGameScore(
        team.map(_.id),
        Some(ScopaGameScoreItem.Scope(team.foldRight(0)(_.extraPoints + _))).filterNot(_.count == 0).toList ++ points
      )
    )

  def calculatePrimiera(cards: List[VisibleCard]): Option[ScopaGameScoreItem.Primiera] =

    def valueOf(card: Card): Int =
      card.rank match {
        case Sette => 21
        case Sei => 18
        case Asso => 16
        case Cinque => 15
        case Quattro => 14
        case Tre => 13
        case Due => 12
        case Re | Cavallo | Fante => 10
      }

    def bestCard(cards: List[VisibleCard]): (VisibleCard, Int) =
      cards.map(card => card -> valueOf(card)).maxBy(_._2)

    val groupedBySuit: Map[Suit, List[VisibleCard]] = cards.groupBy(_.suit)

    for {
      denari <- groupedBySuit.get(Denari)
      spade <- groupedBySuit.get(Spade)
      coppe <- groupedBySuit.get(Coppe)
      bastoni <- groupedBySuit.get(Bastoni)
      bestCards = List(denari, spade, coppe, bastoni).map(bestCard)
    } yield ScopaGameScoreItem.Primiera(bestCards.map(_._1), bestCards.foldRight(0)(_._2 + _))

  given Codec[GameScore] = deriveCodec
