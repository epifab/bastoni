package bastoni.domain.logic
package generic

import bastoni.domain.model.{GameType, MatchInfo, MatchScore}
import io.circe.{Encoder, Json}

class StateMachine[State: Encoder](gameLogic: GameLogic[State], state: State) extends GameStateMachine:
  override def apply(message: StateMachineInput): (Option[GameStateMachine], List[StateMachineOutput]) =
    gameLogic.play(state, message) match
      case (state, events) if gameLogic.isFinal(state) => None -> events
      case (state, events) => Some(new StateMachine(gameLogic, state)) -> events

  override val gameType: GameType = gameLogic.gameType
  override val encoded: Json = Encoder[State].apply(state)
