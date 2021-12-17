package bastoni.backend.tressette

import bastoni.domain.model.{GamePlayer, Player}

sealed trait GameState

object GameState:
  def apply(players: List[Player], pointsToWin: Int = 21): InProgress =
    val gamePlayers = players.map(GamePlayer(_, 0))
    InProgress(gamePlayers, MatchState.Ready(gamePlayers), pointsToWin)

  case class  InProgress(players: List[GamePlayer], matchState: MatchState, pointsToWin: Int) extends GameState
  case object Terminated extends GameState
