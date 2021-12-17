package bastoni.domain.view

import bastoni.domain.model.{Card, GameType, PlayerId}

sealed trait FromPlayer

object FromPlayer:
  case object Connect extends FromPlayer
  case object JoinTable extends FromPlayer
  case object LeaveTable extends FromPlayer
  case class  StartGame(gameType: GameType) extends FromPlayer
  case object ShuffleDeck extends FromPlayer
  case class  PlayCard(card: Card) extends FromPlayer
