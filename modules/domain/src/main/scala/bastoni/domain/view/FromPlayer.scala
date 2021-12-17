package bastoni.domain.view

import bastoni.domain.model.{Card, GameType, PlayerId}

sealed trait FromPlayer

object FromPlayer:
  case object JoinRoom extends FromPlayer
  case object LeaveRoom extends FromPlayer
  // case object Observe extends FromPlayer
  case class  ActivateRoom(gameType: GameType) extends FromPlayer
  case object ShuffleDeck extends FromPlayer
  case class  PlayCard(card: Card) extends FromPlayer
