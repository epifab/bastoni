package bastoni.domain.model

import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*

trait Score:
  def playerIds: List[UserId]
  def points: Int


case class BriscolaScoreItem(card: Card, points: 2 | 3 | 4 | 10 | 11)

case class BriscolaScore(playerIds: List[UserId], items: List[BriscolaScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.points + _)

object BriscolaScore:
  def apply(players: List[Player]): BriscolaScore =
    new BriscolaScore(
      players.map(_.id),
      players.flatMap(_.taken).collect {
        case card@ Card(Asso, _) => BriscolaScoreItem(card, 11)
        case card@ Card(Tre, _) => BriscolaScoreItem(card, 10)
        case card@ Card(Re, _) => BriscolaScoreItem(card, 4)
        case card@ Card(Cavallo, _) => BriscolaScoreItem(card, 3)
        case card@ Card(Fante, _) => BriscolaScoreItem(card, 2)
      }
    )


sealed trait TressetteScoreItem:
  def thirds: 1 | 3

object TressetteScoreItem:
  case class Taken(card: Card, thirds: 1 | 3) extends TressetteScoreItem
  case object Rete extends TressetteScoreItem:
    override val thirds = 3

case class TressetteScore(playerIds: List[UserId], items: List[TressetteScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.thirds + _) / 3

object TressetteScore:
  def apply(players: List[Player], rete: Boolean): TressetteScore =
    new TressetteScore(
      players.map(_.id),
      Option.when(rete)(TressetteScoreItem.Rete).toList ++ players.flatMap(_.taken).collect {
        case card@ Card(Asso, _) => TressetteScoreItem.Taken(card, 3)
        case card@ Card(Tre, _) => TressetteScoreItem.Taken(card, 1)
        case card@ Card(Due, _) => TressetteScoreItem.Taken(card, 1)
        case card@ Card(Re, _) => TressetteScoreItem.Taken(card, 1)
        case card@ Card(Cavallo, _) => TressetteScoreItem.Taken(card, 1)
        case card@ Card(Fante, _) => TressetteScoreItem.Taken(card, 1)
      }
    )


sealed trait ScopaScoreItem(val points: Int)

object ScopaScoreItem:
  case class Carte(count: Int) extends ScopaScoreItem(1)
  case class Denari(count: Int) extends ScopaScoreItem(1)
  case object SetteBello extends ScopaScoreItem(1)
  case class Primiera(cards: List[Card], count: Int) extends ScopaScoreItem(1)
  case class Scope(count: Int) extends ScopaScoreItem(count)

case class ScopaScore(playerIds: List[UserId], items: List[ScopaScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.points + _)

object ScopaScore:

  extension[T](xs: List[T])
    def bestBy[U](f: T => U)(using ord: Ordering[U]): Option[T] =
      xs.map(x => x -> f(x)).sortBy(_._2) match
        case (i1, v1) :: (i2, v2) :: _ if v1 != v2 => Some(i1)
        case (i, _) :: Nil => Some(i)
        case _ => None

  def apply(teams: List[List[Player]]): List[ScopaScore] =
    val teamWithCards: Map[List[Player], List[Card]] = teams.map(team => team -> team.flatMap(_.taken)).toMap

    val carte: Option[(List[Player], ScopaScoreItem)] =
      teamWithCards
        .view.mapValues(cards => ScopaScoreItem.Carte(cards.size))
        .toList.bestBy(_._2.count)

    val denari: Option[(List[Player], ScopaScoreItem)] =
      teamWithCards
        .view.mapValues(cards => ScopaScoreItem.Denari(cards.filter(_.rank == Denari).size))
        .toList.bestBy(_._2.count)

    val primiera: Option[(List[Player], ScopaScoreItem)] =
      teamWithCards
        .flatMap { case (team, cards) => calculatePrimiera(cards).map(team -> _) }
        .maxByOption(_._2.count)

    val setteBello: Option[(List[Player], ScopaScoreItem)] =
      teamWithCards
        .collectFirst { case (team, cards) if cards.contains(Card(Sette, Denari)) =>
          team -> ScopaScoreItem.SetteBello
        }

    teams.map(team =>
      val points =
        List(carte, denari, primiera, setteBello)
          .map(_.collect { case (t, s) if t == team => s })
          .flatten

      new ScopaScore(
        team.map(_.id),
        ScopaScoreItem.Scope(team.foldRight(0)(_.extraPoints + _)) :: points
      )
    )

  def calculatePrimiera(cards: List[Card]): Option[ScopaScoreItem.Primiera] =

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

    def bestCard(cards: List[Card]): (Card, Int) =
      cards.map(card => card -> valueOf(card)).maxBy(_._2)

    val groupedBySuit: Map[Suit, List[Card]] = cards.groupBy(_.suit)

    for {
      denari <- groupedBySuit.get(Denari)
      spade <- groupedBySuit.get(Spade)
      coppe <- groupedBySuit.get(Coppe)
      bastoni <- groupedBySuit.get(Bastoni)
      bestCards = List(denari, spade, coppe, bastoni).map(bestCard)
    } yield ScopaScoreItem.Primiera(bestCards.map(_._1), bestCards.foldRight(0)(_._2 + _))
