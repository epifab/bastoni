package bastoni.domain.model

import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder, Json}

extension[T](xs: List[T])
  def bestBy[U](f: T => U)(using ord: Ordering[U]): Option[T] =
    xs.map(x => x -> f(x)).sortBy(_._2)(using ord.reverse) match
      case (i1, v1) :: (i2, v2) :: _ if v1 != v2 => Some(i1)
      case (i, _) :: Nil => Some(i)
      case _ => None


extension(scores: List[Score])
  def bestTeam: List[UserId] = scores.bestBy(_.points).map(_.playerIds).getOrElse(Nil)

sealed trait Score:
  def playerIds: List[UserId]
  def points: Int


sealed trait GameScore extends Score


// ------------------------------------------------------
//  Briscola
// ------------------------------------------------------

case class BriscolaGameScoreItem(card: VisibleCard, points: Int)

object BriscolaGameScoreItem:
  given Codec[BriscolaGameScoreItem] = deriveCodec

case class BriscolaGameScore(playerIds: List[UserId], items: List[BriscolaGameScoreItem]) extends GameScore:
  override val points: Int = items.foldRight(0)(_.points + _)


// ------------------------------------------------------
//  Tressette
// ------------------------------------------------------

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

case class TressetteGameScore(playerIds: List[UserId], items: List[TressetteGameScoreItem]) extends GameScore:
  override val points: Int = items.foldRight(0)(_.thirds + _) / 3


// ------------------------------------------------------
//  Scopa
// ------------------------------------------------------

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

case class ScopaGameScore(playerIds: List[UserId], items: List[ScopaGameScoreItem]) extends GameScore:
  override val points: Int = items.foldRight(0)(_.points + _)


object GameScore:
  given Encoder[GameScore] = Encoder.instance {
    case s: BriscolaGameScore => deriveEncoder[BriscolaGameScore].mapJsonObject(_.add("type", GameType.Briscola.asJson))(s)
    case s: TressetteGameScore => deriveEncoder[TressetteGameScore].mapJsonObject(_.add("type", GameType.Tressette.asJson))(s)
    case s: ScopaGameScore => deriveEncoder[ScopaGameScore].mapJsonObject(_.add("type", GameType.Scopa.asJson))(s)
  }

  given Decoder[GameScore] = Decoder.instance(cursor => cursor.downField("type").as[GameType].flatMap {
    case GameType.Briscola => deriveDecoder[BriscolaGameScore].tryDecode(cursor)
    case GameType.Tressette => deriveDecoder[TressetteGameScore].tryDecode(cursor)
    case GameType.Scopa => deriveDecoder[ScopaGameScore].tryDecode(cursor)
  })


case class MatchScore(playerIds: List[UserId], points: Int) extends Score

object MatchScore:
  given Encoder[MatchScore] = deriveEncoder
  given Decoder[MatchScore] = deriveDecoder

  def forTeams(teams: List[List[MatchPlayer]]): List[MatchScore] =
    teams.map(players => MatchScore(players.map(_.id), players.headOption.fold(0)(_.points)))
