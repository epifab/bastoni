package bastoni.domain.logic
package generic

import bastoni.domain.model.Event.MatchStarted
import bastoni.domain.model.*
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder}

class StateMachineFactory(gameLogic: GenericGameLogic) extends GameStateMachineFactory:
  override def apply(room: RoomServerView): (GameStateMachine, List[StateMachineOutput]) =
    val players: List[MatchPlayer] = room.round.map(_.player).map(MatchPlayer(_, 0))
    val state: MatchState = gameLogic.initialState(players)
    val machine = new generic.StateMachine(gameLogic, state)

    val event = MatchStarted(
      gameType = gameLogic.gameType,
      matchScores = MatchScore.forTeams(Teams(players))
    )

    machine -> List(event)

  override def decode(json: ACursor): Either[DecodingFailure, GameStateMachine] =
    Decoder[MatchState].tryDecode(json).map(new generic.StateMachine(gameLogic, _))
