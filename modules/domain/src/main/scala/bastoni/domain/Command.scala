package bastoni.domain

sealed trait Command

case class  JoinRoom(player: Player) extends Command
case class  LeaveRoom(player: Player) extends Command
case class  ActivateRoom(player: Player, gameType: GameType) extends Command
case class  PlayCard(player: PlayerId, card: Card) extends Command

case class  StartGame(room: Room, gameType: GameType) extends Command
case class  ShuffleDeck(seed: Int) extends Command
case object Continue extends Command
