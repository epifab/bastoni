package bastoni.domain.logic
package scopa

import bastoni.domain.model.{Command, MatchPlayer, ServerEvent, User}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait ScopaMatchState

object ScopaMatchState:
  def apply(users: List[User]): InProgress =
    val players = users.map(MatchPlayer(_, 0))
    InProgress(players, ScopaGameState.Ready(players), if (players.size == 4) 21 else 11)

  case class  InProgress(players: List[MatchPlayer], game: ScopaGameState, pointsToWin: Int) extends ScopaMatchState with ActiveMatch
  case class  GameOver(output: StateMachineOutput, next: ScopaMatchState) extends ScopaMatchState
  case object Terminated extends ScopaMatchState

  import StateMachineOutput.{encoder, decoder}

  given Encoder[ScopaMatchState] = Encoder.instance {
    case s: InProgress => deriveEncoder[InProgress].mapJsonObject(_.add("stage", "InProgress".asJson))(s)
    case s: GameOver => deriveEncoder[GameOver].mapJsonObject(_.add("stage", "GameOver".asJson))(s)
    case Terminated    => Json.obj("stage" -> "Terminated".asJson)
  }

  given Decoder[ScopaMatchState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "InProgress" => deriveDecoder[InProgress](cursor)
    case "GameOver" => deriveDecoder[GameOver](cursor)
    case "Terminated" => Right(Terminated)
  })
