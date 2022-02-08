package bastoni.domain.logic

import bastoni.domain.model.MatchPlayer

sealed trait GameStatus

object GameStatus:
  case object InProgress extends GameStatus
  case class Completed(players: List[MatchPlayer]) extends GameStatus
  case object Aborted extends GameStatus
