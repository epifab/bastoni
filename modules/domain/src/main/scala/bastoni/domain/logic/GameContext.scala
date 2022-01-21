package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class GameContext(table: TableServerView, stateMachine: Option[GameStateMachine]) extends StateMachine[GameContext]:

  override def apply(message: ServerEvent | Command): (Option[GameContext], List[ServerEvent | Command | Delayed[Command]]) =

    val (updatedTable, tableEvents) = message match {
      case Command.Connect =>
        table -> List(Snapshot(table))

      case Command.JoinTable(user, seed) =>
        table.join(user, seed) match {
          case Right((newTable, seat)) => newTable -> List(PlayerJoinedTable(user, seat))
          case Left(error) => table -> Nil
        }

      case Command.LeaveTable(user) =>
        table.leave(user) match {
          case Right((newTable, seat)) => newTable -> List(PlayerLeftTable(user, seat))
          case Left(error) => table -> Nil
        }

      case _ => table -> Nil
    }

    val (newStateMachine, gameMessages) = (stateMachine, message) match
      case (None, Command.StartMatch(playerId, gameType)) if updatedTable.contains(playerId) && updatedTable.round.size > 1 =>
        val (machine, initialEvents) = GameStateMachineFactory(gameType)(updatedTable)
        Some(machine) -> initialEvents
      case (None, _) => stateMachine -> Nil
      case (Some(stateMachine), message) => stateMachine(message)

    // Play all game events to the table
    val latestTable = gameMessages
      .collect { case event: ServerEvent => event }
      .foldLeft(updatedTable)(_ update _)

    val allEvents = tableEvents ++ gameMessages

    Option.when(latestTable.nonEmpty || newStateMachine.isDefined)(GameContext(latestTable, newStateMachine)) -> allEvents


object GameContext:

  def build(tableSize: 2 | 3 | 4): GameContext =
    GameContext(
      table = TableServerView(
        seats = (0 until tableSize).map(index => EmptySeat(index, Nil, Nil)).toList,
        deck = Nil,
        board = Nil,
        matchInfo = None,
        dealerIndex = None
      ),
      stateMachine = None
    )

  given Codec[GameContext] = deriveCodec
