package bastoni.domain

sealed trait Event

case class  PlayerJoined(player: Player, room: Room) extends Event
case class  PlayerLeft(player: Player, room: Room) extends Event

case class  GameStart(player1: Player, player2: Player) extends Event
case object GameTermination extends Event
case class  ShuffleDeck(seed: Int) extends Event
case class  DealCard(playerId: PlayerId, card: Card) extends Event
case class  CollectCards(playerId: PlayerId, cards: List[Card]) extends Event
case class  Score(playerId: PlayerId, points: Int)
