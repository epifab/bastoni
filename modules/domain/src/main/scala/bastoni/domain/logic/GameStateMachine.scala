package bastoni.domain.logic

import bastoni.domain.model.*
import io.circe.syntax.EncoderOps
import io.circe.*


trait GameStateMachineFactory:
  def apply(room: RoomServerView): (GameStateMachine, List[StateMachineOutput])
  def decode(json: ACursor): Either[DecodingFailure, GameStateMachine]


object GameStateMachineFactory:
  def apply(gameType: GameType): GameStateMachineFactory =
    gameType match
      case GameType.Briscola => briscola.StateMachine
      case GameType.Tressette => tressette.StateMachine
      case GameType.Scopa => scopa.StateMachine


trait GameStateMachine extends StateMachine[GameStateMachine]:
  def gameType: GameType
  def encoded: Json


object GameStateMachine:
  given Encoder[GameStateMachine] = Encoder.instance { machine =>
    Json.obj(
      "game" -> machine.gameType.asJson,
      "state" -> machine.encoded
    )
  }

  given Decoder[GameStateMachine] = Decoder.instance(obj =>
    for {
      game <- obj.downField("game").as[GameType]
      stateMachine <- GameStateMachineFactory(game).decode(obj.downField("state"))
    } yield stateMachine
  )
