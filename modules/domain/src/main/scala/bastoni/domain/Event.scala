package bastoni.domain

sealed trait Event

case class  PlayerJoined(player: Player, room: Room) extends Event
case class  PlayerLeft(player: Player, room: Room) extends Event

case class  GameStart(player1: Player, player2: Player) extends Event
case object GameAborted extends Event
case class  DeckShuffled(seed: Int) extends Event
case class  CardDealt(playerId: PlayerId, card: Card) extends Event
case class  TrumpRevealed(card: Card) extends Event
case class  CardPlayed(playerId: PlayerId, card: Card) extends Event
case class  TrickWinner(playerId: PlayerId) extends Event
case class  PointsCount(playerId: PlayerId, points: Int) extends Event
case class  MatchWinner(playerId: PlayerId) extends Event
case object MatchDraw extends Event
case class  GameWinner(player: PlayerId) extends Event
