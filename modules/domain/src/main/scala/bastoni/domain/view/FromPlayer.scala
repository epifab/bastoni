package bastoni.domain.view

import bastoni.domain.model.{GameType, VisibleCard}

sealed trait FromPlayer

object FromPlayer:
  case object Connect extends FromPlayer
  case object JoinRoom extends FromPlayer
  case object LeaveRoom extends FromPlayer
  case class  StartMatch(gameType: GameType) extends FromPlayer
  case object ShuffleDeck extends FromPlayer
  case class  PlayCard(card: VisibleCard) extends FromPlayer
  case class  TakeCards(card: VisibleCard, take: List[VisibleCard]) extends FromPlayer
