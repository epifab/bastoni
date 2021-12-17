package bastoni.domain.logic.briscola

import bastoni.domain.model.Rank.{Asso, Cavallo, Fante, Re, Tre}
import bastoni.domain.model.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

case class GameScoreItem(card: Card, points: 2 | 3 | 4 | 10 | 11)

case class GameScore(playerIds: List[UserId], items: List[GameScoreItem]) extends Score:
  override val points: Int = items.foldRight(0)(_.points + _)

object GameScore:
  def apply(players: List[Player]): GameScore =
    new GameScore(
      players.map(_.id),
      players.flatMap(_.taken).collect {
        case card@ Card(Asso, _) => GameScoreItem(card, 11)
        case card@ Card(Tre, _) => GameScoreItem(card, 10)
        case card@ Card(Re, _) => GameScoreItem(card, 4)
        case card@ Card(Cavallo, _) => GameScoreItem(card, 3)
        case card@ Card(Fante, _) => GameScoreItem(card, 2)
      }
    )

  given Encoder[2 | 3 | 4 | 10 | 11] = Encoder.encodeInt.contramap {
    case 2  => 2
    case 3  => 3
    case 4  => 4
    case 10 => 10
    case 11 => 11
  }
  given Decoder[2 | 3 | 4 | 10 | 11] = Decoder.decodeInt.emap {
    case 2  => Right(2)
    case 3  => Right(3)
    case 4  => Right(4)
    case 10 => Right(10)
    case 11 => Right(11)
    case x  => Left(s"Invalid value $x")
  }
  given Codec[GameScoreItem] = deriveCodec
  given Codec[GameScore] = deriveCodec
