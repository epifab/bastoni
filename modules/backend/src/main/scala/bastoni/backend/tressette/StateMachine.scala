package bastoni.backend
package tressette

import bastoni.domain.model.{Command, Event, Player, PlayerId}

class StateMachine private(state: GameState) extends GameStateMachine:
  override def apply(message: Command | Event): (Option[StateMachine], List[Event | Command | Delayed[Command]]) =
    Game.playGameStep(state, message) match
      case (GameState.Terminated, events) => None -> events
      case (state, events) => Some(new StateMachine(state)) -> events

object StateMachine:
  def apply(players: List[Player]): StateMachine =
    new StateMachine(GameState(players))
