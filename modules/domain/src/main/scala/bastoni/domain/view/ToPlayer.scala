package bastoni.domain.view

import bastoni.domain.model.*

sealed trait ToPlayer

object ToPlayer:
  sealed trait RoomMessage:
    def room: Room
  case class   PlayerJoined(player: Player, room: Room) extends ToPlayer with RoomMessage
  case class   PlayerLeft(player: Player, room: Room) extends ToPlayer with RoomMessage
  // case class   Snapshot(table: TableView) extends ToPlayer
  case class   GameStarted(gameType: GameType) extends ToPlayer
  case class   DeckShuffled(cards: Int) extends ToPlayer
  case class   CardDealt(playerId: PlayerId, card: Option[Card]) extends ToPlayer
  case class   TrumpRevealed(card: Card) extends ToPlayer
  case class   CardPlayed(playerId: PlayerId, card: Card) extends ToPlayer
  case class   TrickCompleted(winner: PlayerId) extends ToPlayer
  case class   MatchCompleted(winnerIds: List[PlayerId], matchPoints: List[PointsCount], gamePoints: List[PointsCount]) extends ToPlayer
  case object  MatchAborted extends ToPlayer
  case class   GameCompleted(winnerIds: List[PlayerId]) extends ToPlayer
  case object  GameAborted extends ToPlayer
  case class   ActionRequest(playerId: PlayerId, action: Command.Action) extends ToPlayer
