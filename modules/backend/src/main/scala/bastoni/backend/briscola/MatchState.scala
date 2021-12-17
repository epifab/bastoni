package bastoni.backend.briscola

import bastoni.backend.{GamePlayer, MatchPlayer}
import bastoni.domain.model.{Card, Player, PlayerId}

sealed trait MatchState

object MatchState:
  def apply(players: List[Player]): Ready =
    Ready(players.map(GamePlayer(_, 0)))

  case class   Ready(players: List[GamePlayer]) extends MatchState
  case class   DealRound(todo: List[MatchPlayer], done: List[MatchPlayer], remaining: Int, deck: List[Card]) extends MatchState
  case class   WillDealTrump(players: List[MatchPlayer], deck: List[Card]) extends MatchState
  case class   DrawRound(todo: List[MatchPlayer], done: List[MatchPlayer], deck: List[Card], trump: Card) extends MatchState
  case class   PlayRound(todo: List[MatchPlayer], done: List[(MatchPlayer, Card)], deck: List[Card], trump: Card) extends MatchState
  case class   WillCompleteTrick(players: List[(MatchPlayer, Card)], deck: List[Card], trump: Card) extends MatchState
  case class   WillComplete(players: List[MatchPlayer], trump: Card) extends MatchState
  sealed trait Terminated extends MatchState
  case class   Completed(winners: List[PlayerId]) extends Terminated
  case object  Aborted extends Terminated
