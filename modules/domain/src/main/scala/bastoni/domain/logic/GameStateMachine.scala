package bastoni.domain.logic

import bastoni.domain.model.*
import io.circe.*
import io.circe.syntax.EncoderOps

trait GameStateMachineFactory:
  def apply(room: RoomServerView): (GameStateMachine, List[StateMachineOutput])
  def decode(json: ACursor): Either[DecodingFailure, GameStateMachine]

object GameStateMachineFactory:
  def apply(gameType: GameType): GameStateMachineFactory =
    gameType match
      case GameType.Briscola  => briscola.BriscolaStateMachine
      case GameType.Tressette => tressette.TressetteStateMachine
      case GameType.Scopa     => scopa.ScopaStateMachine

trait GameStateMachine extends StateMachine[GameStateMachine]:
  def gameType: GameType
  def encoded: Json

object GameStateMachine:
  given Encoder[GameStateMachine] = Encoder.instance { machine =>
    Json.obj(
      "game"  -> machine.gameType.asJson,
      "state" -> machine.encoded
    )
  }

  given Decoder[GameStateMachine] = Decoder.instance(obj =>
    for
      game         <- obj.downField("game").as[GameType]
      stateMachine <- GameStateMachineFactory(game).decode(obj.downField("state"))
    yield stateMachine
  )
