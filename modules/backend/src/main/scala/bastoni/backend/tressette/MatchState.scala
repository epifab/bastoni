package bastoni.backend.tressette

import bastoni.domain.model.*

sealed trait MatchState

object MatchState:
  def apply(players: List[Player]): Ready =
    Ready(players.map(GamePlayer(_, 0)))

  case class   Ready(players: List[GamePlayer]) extends MatchState
  case class   DealRound(todo: List[MatchPlayer], done: List[MatchPlayer], remaining: Int, deck: List[Card]) extends MatchState
  case class   DrawRound(todo: List[MatchPlayer], done: List[MatchPlayer], deck: List[Card]) extends MatchState
  case class   PlayRound(todo: List[MatchPlayer], done: List[(MatchPlayer, Card)], deck: List[Card]) extends MatchState
  case class   WillCompleteTrick(players: List[(MatchPlayer, Card)], deck: List[Card]) extends MatchState
  case class   WillComplete(players: List[MatchPlayer]) extends MatchState
  sealed trait Terminated extends MatchState
  case class   Completed(winners: List[PlayerId]) extends Terminated
  case object  Aborted extends Terminated
