package bastoni.domain.view

import bastoni.domain.model.Command.Act
import bastoni.domain.model.{PlayerEvent, RoomPlayerView}

sealed trait ToPlayer

object ToPlayer:
  case class Snapshot(room: RoomPlayerView) extends ToPlayer
  case class GameEvent(event: PlayerEvent) extends ToPlayer
  case class Request(act: Act) extends ToPlayer
