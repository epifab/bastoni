package bastoni.backend.briscola

import bastoni.backend.GameStateMachine
import bastoni.domain.{Command, Event, Player}

class StateMachine private(state: GameState) extends GameStateMachine:
  override def apply(message: Command | Event): (Option[StateMachine], List[Event]) =
    Game.playGameStep(state, message) match
      case (GameState.Terminated, events) => None -> events
      case (state, events) => Some(new StateMachine(state)) -> events

object StateMachine:
  def apply(players: List[Player]): StateMachine =
    new StateMachine(GameState(players))
