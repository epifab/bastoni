package bastoni.domain.logic

import bastoni.domain.model.Command.Continue
import bastoni.domain.model.*

extension (message: Event | Command)
  def toMessage(roomId: RoomId): Message =
    Message(Fixtures.messageId, roomId, message)

extension (message: Event | Command | Delayed[Command])
  def toMessage(roomId: RoomId): Message | Delayed[Message] =
    message match
      case event: Event => event.toMessage(roomId)
      case command: Command => command.toMessage(roomId)
      case Delayed(command: Command, delay) => Delayed(command.toMessage(roomId), delay)

object Fixtures:
  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")
  val player4 = Player(PlayerId.newId, "Giuda")

  val messageId: MessageId = MessageId.newId
  val messageIds: fs2.Stream[fs2.Pure, MessageId] = fs2.Stream.constant(messageId)

  val shortDelay: Delayed[Command] = Delayed(Continue, Delay.Short)
  val mediumDelay: Delayed[Command] = Delayed(Continue, Delay.Medium)
  val longDelay: Delayed[Command] = Delayed(Continue, Delay.Long)
