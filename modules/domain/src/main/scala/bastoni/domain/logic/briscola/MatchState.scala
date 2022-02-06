package bastoni.domain.logic.briscola

import bastoni.domain.logic.{ActiveMatch, StateMachineOutput}
import bastoni.domain.model.{Command, MatchPlayer, ServerEvent, User}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait MatchState

object MatchState:
  def apply(players: List[User]): InProgress =
    val matchPlayers = players.map(MatchPlayer(_, 0))
    InProgress(matchPlayers, GameState.Ready(matchPlayers), 2)

  case class  InProgress(players: List[MatchPlayer], game: GameState, remainingGames: Int) extends MatchState with ActiveMatch
  case class  GameOver(output: StateMachineOutput, next: MatchState) extends MatchState
  case object Terminated extends MatchState

  import StateMachineOutput.{encoder, decoder}

  given Encoder[MatchState] = Encoder.instance {
    case s: InProgress => deriveEncoder[InProgress].mapJsonObject(_.add("stage", "InProgress".asJson))(s)
    case s: GameOver => deriveEncoder[GameOver].mapJsonObject(_.add("stage", "GameOver".asJson))(s)
    case Terminated    => Json.obj("stage" -> "Terminated".asJson)
  }

  given Decoder[MatchState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "InProgress" => deriveDecoder[InProgress](cursor)
    case "GameOver" => deriveDecoder[GameOver](cursor)
    case "Terminated" => Right(Terminated)
  })
