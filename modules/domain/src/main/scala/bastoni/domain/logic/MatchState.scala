package bastoni.domain.logic

import bastoni.domain.logic.generic.*
import bastoni.domain.model.{MatchPlayer, MatchScore, Teams}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

sealed trait MatchType

object MatchType:
  case class FixedRounds(remainingGames: Int) extends MatchType
  case class PointsBased(pointsToWin: Int) extends MatchType

  given Encoder[MatchType] = Encoder.instance {
    case obj: FixedRounds => deriveEncoder[FixedRounds].mapJsonObject(_.add("type", "FixedRounds".asJson)).apply(obj)
    case obj: PointsBased => deriveEncoder[PointsBased].mapJsonObject(_.add("type", "PointsBased".asJson)).apply(obj)
  }

  given Decoder[MatchType] = Decoder.instance { obj =>
    val typeCursor = obj.downField("type")

    typeCursor.as[String].flatMap {
      case "FixedRounds" => deriveDecoder[FixedRounds].tryDecode(obj)
      case "PointsBased" => deriveDecoder[PointsBased].tryDecode(obj)
      case unknown => Left(DecodingFailure(s"Unknown match type $unknown", typeCursor.history))
    }
  }

sealed trait MatchState

object MatchState:
  case class InProgress(players: List[MatchPlayer], gameState: Json, matchType: MatchType) extends MatchState:
    def nextGame(newState: Json, newPlayers: List[MatchPlayer]): InProgress =
      copy(
        gameState = newState,
        players = newPlayers,
        matchType = matchType match {
          case MatchType.FixedRounds(remainingGames) => MatchType.FixedRounds(remainingGames - 1)
          case pointsBased: MatchType.PointsBased => pointsBased
        }
      )

  case class   GameOver(output: StateMachineOutput, next: MatchState) extends MatchState
  case object  Terminated extends MatchState

  import StateMachineOutput.{decoder, encoder}

  given Encoder[MatchState] = Encoder.instance {
    case s: InProgress =>
      Json.obj(
        "stage" -> "InProgress".asJson,
        "gameState" -> s.gameState,
        "players" -> s.players.asJson,
        "matchType" -> s.matchType.asJson
      )

    case s: GameOver => deriveEncoder[GameOver].mapJsonObject(_.add("stage", "GameOver".asJson))(s)
    case Terminated    => Json.obj("stage" -> "Terminated".asJson)
  }

  given Decoder[MatchState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "InProgress" =>
      for {
        players <- cursor.downField("players").as[List[MatchPlayer]]
        gameState <- cursor.downField("gameState").focus.toRight(DecodingFailure("Game state not found", cursor.history))
        matchType <- cursor.downField("matchType").as[MatchType]
      } yield InProgress(players, gameState, matchType)
    case "GameOver" => deriveDecoder[GameOver](cursor)
    case "Terminated" => Right(Terminated)
  })
