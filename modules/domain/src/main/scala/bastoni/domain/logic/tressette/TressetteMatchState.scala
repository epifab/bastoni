package bastoni.domain.logic
package tressette

import bastoni.domain.logic.ActiveMatch
import bastoni.domain.model.{Command, MatchPlayer, ServerEvent, User}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait TressetteMatchState

object TressetteMatchState:
  def apply(users: List[User], pointsToWin: Int = 21): InProgress =
    val players = users.map(MatchPlayer(_, 0))
    InProgress(players, TressetteGameState.Ready(players), pointsToWin)

  case class  InProgress(players: List[MatchPlayer], game: TressetteGameState, pointsToWin: Int) extends TressetteMatchState with ActiveMatch
  case class  GameOver(output: StateMachineOutput, next: TressetteMatchState) extends TressetteMatchState
  case object Terminated extends TressetteMatchState

  import StateMachineOutput.{encoder, decoder}

  given Encoder[TressetteMatchState] = Encoder.instance {
    case s: InProgress => deriveEncoder[InProgress].mapJsonObject(_.add("stage", "InProgress".asJson))(s)
    case s: GameOver => deriveEncoder[GameOver].mapJsonObject(_.add("stage", "GameOver".asJson))(s)
    case Terminated    => Json.obj("stage" -> "Terminated".asJson)
  }

  given Decoder[TressetteMatchState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "InProgress" => deriveDecoder[InProgress](cursor)
    case "GameOver" => deriveDecoder[GameOver](cursor)
    case "Terminated" => Right(Terminated)
  })
