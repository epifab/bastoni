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
      case Command.JoinTable(player, seed) =>
        table.join(player, seed) match {
          case Right((newTable, seat)) => newTable -> List(PlayerJoinedTable(player, seat))
          case Left(error) => table -> Nil
        }
      case Command.LeaveTable(player) =>
        table.leave(player) match {
          case Right((newTable, seat)) => newTable -> List(PlayerLeftTable(player, seat))
          case Left(error) => table -> Nil
        }
      case _ => table -> Nil
    }

    val (newStateMachine, gameMessages) = (stateMachine, message) match
      case (None, Command.StartGame(playerId, gameType)) if updatedTable.contains(playerId) && updatedTable.players.size > 1 =>
        Some(GameStateMachineFactory(gameType)(updatedTable)) -> List(GameStarted(gameType))
      case (None, _) => stateMachine -> Nil
      case (Some(stateMachine), message) => stateMachine(message)

    // Play all game events to the table
    val latestTable = gameMessages
      .collect { case event: ServerEvent => event }
      .foldLeft(updatedTable)(_ update _)

    val allEvents = tableEvents ++ gameMessages

    Option.when(latestTable.nonEmpty || newStateMachine.isDefined)(GameContext(latestTable, newStateMachine)) -> allEvents


object GameContext:

  def build(tableSize: Int): GameContext =
    GameContext(TableServerView(List.fill(4)(Seat(None, Nil, Nil, Nil)), Nil, Nil, false), None)

  given Codec[GameContext] = deriveCodec
