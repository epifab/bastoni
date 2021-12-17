package bastoni.domain.logic
package generic

import bastoni.domain.model.TableServerView
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder}

class StateMachineFactory[State: Decoder: Encoder](gameLogic: GameLogic[State]) extends GameStateMachineFactory:
  override def apply(table: TableServerView): GameStateMachine =
    new generic.StateMachine(gameLogic, gameLogic.initialState(table.players))

  override def decode(json: ACursor): Either[DecodingFailure, GameStateMachine] =
    Decoder[State].tryDecode(json).map(new generic.StateMachine(gameLogic, _))
