package bastoni.domain.view

import bastoni.domain.model.{Card, GameType}

sealed trait Command

case object JoinRoom extends Command
case object LeaveRoom extends Command
case class  ActivateRoom(gameType: GameType) extends Command
case object ShuffleDeck extends Command
case class  PlayCard(card: Card) extends Command
