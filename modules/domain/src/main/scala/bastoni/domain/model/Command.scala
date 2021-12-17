package bastoni.domain.model

sealed trait Command

object Command:
  case class  JoinRoom(player: Player) extends Command
  case class  LeaveRoom(player: Player) extends Command
  case class  ActivateRoom(player: Player, gameType: GameType) extends Command
  case class  StartGame(room: Room, gameType: GameType) extends Command
  case class  ShuffleDeck(seed: Int) extends Command
  case class  PlayCard(player: PlayerId, card: Card) extends Command
  case object Continue extends Command

  enum Action:
    case PlayCard
    case PlayCardOf(suit: Suit)
    case ShuffleDeck

  case class  ActionRequest(playerId: PlayerId, action: Action) extends Command
