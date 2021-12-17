package bastoni.domain

sealed trait Event

case class  PlayerJoined(player: Player, room: Room) extends Event
case class  PlayerLeft(player: Player, room: Room) extends Event

case class  DeckShuffled(seed: Int) extends Event
case class  CardDealt(playerId: PlayerId, card: Card) extends Event
case class  TrumpRevealed(card: Card) extends Event
case class  CardPlayed(playerId: PlayerId, card: Card) extends Event
case class  TrickWinner(playerId: PlayerId) extends Event
case class  PointsCount(playerIds: List[PlayerId], points: Int) extends Event
case class  MatchWinners(playerIds: List[PlayerId]) extends Event
case object MatchDraw extends Event
case object MatchAborted extends Event
