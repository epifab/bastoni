package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import cats.implicits.showInterpolator
import cats.syntax.show
import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class GameContext(room: RoomServerView, stateMachine: Option[GameStateMachine]) extends StateMachine[GameContext]:

  override def apply(
      message: ServerEvent | Command
  ): (Option[GameContext], List[ServerEvent | Command | Delayed[Command]]) =

    val (updatedRoom, roomEvents) = message match
      case Command.Connect(user) =>
        room -> List(PlayerConnected(user, room))

      case Command.JoinTable(user, seed) =>
        room.join(user, seed) match
          case Right((newRoom, seat)) => newRoom -> List(PlayerJoinedTable(user, seat))
          case Left(error)            => room    -> List(ClientError(s"Failed to join the table: $error"))

      case Command.LeaveTable(user) =>
        room.leave(user) match
          case Right((newRoom, seat)) => newRoom -> List(PlayerLeftTable(user, seat))
          case Left(error)            => room    -> List(ClientError(s"Failed to leave the table: $error"))

      case _ => room -> Nil

    val (newStateMachine, gameMessages) = (stateMachine, message) match
      case (None, Command.StartMatch(playerId, gameType)) =>
        if !updatedRoom.contains(playerId)
        then stateMachine -> (ClientError(show"Player $playerId cannot start a match: not in the room") :: Nil)
        else if updatedRoom.round.size <= 1
        then stateMachine -> (ClientError(show"Cannot start a match in an empty room") :: Nil)
        else
          GameStateMachineFactory(gameType)(updatedRoom) match
            case (m, e) => Some(m) -> e
      case (None, _)                     => stateMachine -> Nil
      case (Some(stateMachine), message) => stateMachine(message)

    // Play all game events to the room
    val latestRoom = gameMessages
      .collect { case event: (ServerEvent | Command.Act) => event }
      .foldLeft(updatedRoom) {
        case (room, event: ServerEvent)   => room.update(event)
        case (room, command: Command.Act) => room.withRequest(command)
      }

    val allEvents = roomEvents ++ gameMessages

    Option.when(latestRoom.nonEmpty || newStateMachine.isDefined)(GameContext(latestRoom, newStateMachine)) -> allEvents
  end apply
end GameContext

object GameContext:

  def build(roomSize: 2 | 3 | 4): GameContext =
    GameContext(
      room = RoomServerView(
        seats = (0 until roomSize).map(index => EmptySeat(index, Nil, Nil)).toList,
        deck = Nil,
        board = Nil,
        matchInfo = None,
        dealerIndex = None,
        players = Map.empty
      ),
      stateMachine = None
    )

  given Codec[GameContext] = deriveCodec
