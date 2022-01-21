package bastoni.domain.logic
package generic

import bastoni.domain.model.Event.MatchStarted
import bastoni.domain.model.{MatchInfo, MatchScore, TableServerView, Teams}
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder}

class StateMachineFactory[State: Decoder: Encoder](gameLogic: GameLogic[State]) extends GameStateMachineFactory:
  override def apply(table: TableServerView): (GameStateMachine, List[StateMachineOutput]) =
    val state: State & ActiveMatch = gameLogic.initialState(table.round.map(_.player))
    val machine = new generic.StateMachine(gameLogic, state)
    val event = MatchStarted(
      gameType = gameLogic.gameType,
      matchScores = MatchScore.forTeams(Teams(state.players))
    )
    machine -> List(event)

  override def decode(json: ACursor): Either[DecodingFailure, GameStateMachine] =
    Decoder[State].tryDecode(json).map(new generic.StateMachine(gameLogic, _))
