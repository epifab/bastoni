package bastoni.domain.logic.generic

import bastoni.domain.model.Event.GameAborted
import bastoni.domain.model.MatchPlayer

sealed trait GameStatus

object GameStatus:
  case object InProgress                           extends GameStatus
  case class Completed(players: List[MatchPlayer]) extends GameStatus
  case class Aborted(reason: GameAborted.Reason)   extends GameStatus
