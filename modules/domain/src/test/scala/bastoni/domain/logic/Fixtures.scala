package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.Continue
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*

extension (message: ServerEvent | Command)
  def toMessage(roomId: RoomId): Message =
    Message(Fixtures.messageId, roomId, message)

extension (message: ServerEvent | Command | Delayed[Command])
  def toMessage(roomId: RoomId): Message | Delayed[Message] =
    message match
      case event: ServerEvent => event.toMessage(roomId)
      case command: Command => command.toMessage(roomId)
      case Delayed(command: Command, delay) => Delayed(command.toMessage(roomId), delay)

object Fixtures:
  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")
  val player4 = Player(PlayerId.newId, "Giuda")
  val player5 = Player(PlayerId.newId, "Ultimo")

  val messageId: MessageId = MessageId.newId

  val shortDelay: Delayed[Command] = Delayed(Continue, Delay.Short)
  val mediumDelay: Delayed[Command] = Delayed(Continue, Delay.Medium)
  val longDelay: Delayed[Command] = Delayed(Continue, Delay.Long)

  val shuffleSeed = 10
  val shuffledDeck = List(
    Card(Due, Bastoni),
    Card(Asso, Spade),
    Card(Sette, Denari),
    Card(Quattro, Spade),
    Card(Sei, Denari),
    Card(Re, Denari),
    Card(Cinque, Coppe),
    Card(Asso, Bastoni),
    Card(Cinque, Spade),
    Card(Sei, Bastoni),
    Card(Tre, Spade),
    Card(Tre, Denari),
    Card(Asso, Coppe),
    Card(Fante, Bastoni),
    Card(Due, Denari),
    Card(Fante, Spade),
    Card(Re, Bastoni),
    Card(Sette, Bastoni),
    Card(Tre, Coppe),
    Card(Fante, Coppe),
    Card(Cinque, Bastoni),
    Card(Sei, Coppe),
    Card(Cavallo, Denari),
    Card(Cavallo, Bastoni),
    Card(Due, Coppe),
    Card(Fante, Denari),
    Card(Cavallo, Spade),
    Card(Quattro, Bastoni),
    Card(Re, Coppe),
    Card(Quattro, Coppe),
    Card(Asso, Denari),
    Card(Sette, Spade),
    Card(Cinque, Denari),
    Card(Sette, Coppe),
    Card(Re, Spade),
    Card(Sei, Spade),
    Card(Quattro, Denari),
    Card(Tre, Bastoni),
    Card(Due, Spade),
    Card(Cavallo, Coppe)
  )

  val joinSeed = 4099

//  val seed =
//    fs2.Stream.range(0, 10000)
//      .map(i => i -> scala.util.Random(i).shuffle(List(1, 2, 3, 4)))
//      .find { case (i, l) => l == List(4, 3, 2, 1) }
//      .take(1)
//      .compile
//      .lastOrError
