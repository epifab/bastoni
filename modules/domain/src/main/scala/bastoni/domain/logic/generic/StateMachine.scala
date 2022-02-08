package bastoni.domain.logic
package generic

import bastoni.domain.model.{GameType, MatchInfo, MatchScore}
import io.circe.{Encoder, Json}

class StateMachine[State](gameLogic: GameLogic[State], state: MatchState) extends GameStateMachine:
  override def apply(message: StateMachineInput): (Option[GameStateMachine], List[StateMachineOutput]) =
    gameLogic.playStep(state, message) match
      case (MatchState.Terminated, events) => None -> events
      case (state, events) => Some(new StateMachine(gameLogic, state)) -> events

  override val gameType: GameType = gameLogic.gameType
  override val encoded: Json = Encoder[MatchState].apply(state)
