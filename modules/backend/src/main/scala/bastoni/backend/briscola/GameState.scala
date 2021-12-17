package bastoni.backend.briscola

import bastoni.backend.GamePlayer
import bastoni.domain.model.Player

sealed trait GameState

object GameState:
  def apply(players: List[Player]): InProgress =
    val gamePlayers = players.map(GamePlayer(_, 0))
    InProgress(gamePlayers, MatchState.Ready(gamePlayers), 2)

  case class  InProgress(players: List[GamePlayer], matchState: MatchState, rounds: Int) extends GameState
  case object Terminated extends GameState
