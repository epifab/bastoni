package bastoni.domain.model

sealed trait Event

object Event:
  case class  PlayerJoined(player: Player, room: Room) extends Event
  case class  PlayerLeft(player: Player, room: Room) extends Event

  case class  DeckShuffled(seed: Int) extends Event
  case class  CardDealt(playerId: PlayerId, card: Card) extends Event
  case class  TrumpRevealed(card: Card) extends Event
  case class  CardPlayed(playerId: PlayerId, card: Card) extends Event
  case class  TrickCompleted(winnerId: PlayerId) extends Event
  case class  PointsCount(playerIds: List[PlayerId], points: Int) extends Event
  case class  MatchCompleted(winnerIds: List[PlayerId]) extends Event
  case object MatchDraw extends Event
  case object MatchAborted extends Event
  case class  GameCompleted(winnerIds: List[PlayerId]) extends Event
  case object GameAborted extends Event
