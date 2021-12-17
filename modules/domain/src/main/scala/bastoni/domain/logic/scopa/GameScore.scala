package bastoni.domain.logic.scopa

import bastoni.domain.model.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder, Json}

sealed trait GameScoreItem(val points: Int)

object GameScoreItem:
  case class  Carte(count: Int) extends GameScoreItem(1)
  case class  Denari(count: Int) extends GameScoreItem(1)
  case object SetteBello extends GameScoreItem(1)
  case class  Primiera(cards: List[Card], count: Int) extends GameScoreItem(1)
  case class  Scope(count: Int) extends GameScoreItem(count)

  given Encoder[GameScoreItem] = Encoder.instance {
    case obj: Carte => deriveEncoder[Carte].mapJsonObject(_.add("type", "Carte".asJson))(obj)
    case obj: Denari => deriveEncoder[Denari].mapJsonObject(_.add("type", "Carte".asJson))(obj)
    case SetteBello => Json.obj("type" -> "SetteBello".asJson)
    case obj: Primiera => deriveEncoder[Primiera].mapJsonObject(_.add("type", "Primiera".asJson))(obj)
    case obj: Scope => deriveEncoder[Scope].mapJsonObject(_.add("type", "Scope".asJson))(obj)
  }

  given Decoder[GameScoreItem] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "Carte" => deriveDecoder[Carte].tryDecode(obj)
    case "Denari" => deriveDecoder[Denari].tryDecode(obj)
    case "SetteBello" => Right(SetteBello)
    case "Primiera" => deriveDecoder[Primiera].tryDecode(obj)
    case "Scope" => deriveDecoder[Scope].tryDecode(obj)
  })

case class GameScore(playerIds: List[UserId], items: List[GameScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.points + _)

object GameScore:

  def apply(teams: List[List[Player]]): List[GameScore] =
    val teamWithCards: Map[List[Player], List[Card]] = teams.map(team => team -> team.flatMap(_.taken)).toMap

    val carte: Option[(List[Player], GameScoreItem)] =
      teamWithCards
        .view.mapValues(cards => GameScoreItem.Carte(cards.size))
        .toList.bestBy(_._2.count)

    val denari: Option[(List[Player], GameScoreItem)] =
      teamWithCards
        .view.mapValues(cards => GameScoreItem.Denari(cards.filter(_.rank == Denari).size))
        .toList.bestBy(_._2.count)

    val primiera: Option[(List[Player], GameScoreItem)] =
      teamWithCards
        .flatMap { case (team, cards) => calculatePrimiera(cards).map(team -> _) }
        .maxByOption(_._2.count)

    val setteBello: Option[(List[Player], GameScoreItem)] =
      teamWithCards
        .collectFirst { case (team, cards) if cards.contains(Card(Sette, Denari)) =>
          team -> GameScoreItem.SetteBello
        }

    teams.map(team =>
      val points =
        List(carte, denari, primiera, setteBello)
          .map(_.collect { case (t, s) if t == team => s })
          .flatten

      new GameScore(
        team.map(_.id),
        GameScoreItem.Scope(team.foldRight(0)(_.extraPoints + _)) :: points
      )
    )

  def calculatePrimiera(cards: List[Card]): Option[GameScoreItem.Primiera] =

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
    } yield GameScoreItem.Primiera(bestCards.map(_._1), bestCards.foldRight(0)(_._2 + _))

  given Codec[GameScore] = deriveCodec
