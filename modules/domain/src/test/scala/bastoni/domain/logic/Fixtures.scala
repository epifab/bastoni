package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.{Continue, Tick}
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*

extension (message: ServerEvent | Command)
  def toMessage(roomId: RoomId): Message =
    Message(Fixtures.messageId, roomId, message)

extension (message: ServerEvent | Command | Delayed[Command])
  def toMessage(roomId: RoomId): Message | Delayed[Message] =
    message match
      case event: ServerEvent               => event.toMessage(roomId)
      case command: Command                 => command.toMessage(roomId)
      case Delayed(command: Command, delay) => Delayed(command.toMessage(roomId), delay)

object Fixtures:
  val room1: RoomId = RoomId.unsafeParse("DA97C007-DE93-415E-8705-DD2E1911A651")
  val room2: RoomId = RoomId.unsafeParse("14260D31-75C7-4AC5-B03E-4AFFF99BAF16")

  val user1: User = User(UserId.unsafeParse("6517FC2F-6FED-4169-8C77-17D21492D450"), "Tizio")
  val user2: User = User(UserId.unsafeParse("DC5D9B78-E403-4431-B6F2-92C31D397DB9"), "Caio")
  val user3: User = User(UserId.unsafeParse("FFDBB2B6-C66C-49B6-9C2A-B0DB61FCD1A3"), "Sempronio")
  val user4: User = User(UserId.unsafeParse("1AB05667-7571-40D4-9108-A2C211E308A6"), "Giuda")
  val user5: User = User(UserId.unsafeParse("46CC23E1-751C-4976-9CAF-7A7678297104"), "Ultimo")

  val messageId: MessageId = MessageId.unsafeParse("1FA083E5-802E-476A-B17D-48C81D637B51")

  def willTick(hash: Int): Delayed[Command] = Delayed(Tick(hash), Delay.ActionTimeout)

  val shuffleSeed = 10

  val shuffledDeck: Deck = List(
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
  ).toDeck

  def cardOf(rank: Rank, suit: Suit): VisibleCard =
    shuffledDeck.asList
      .find(c => c.rank == rank && c.suit == suit)
      .getOrElse(throw new IllegalStateException("Card not found"))

  val joinSeed = 4099
end Fixtures
