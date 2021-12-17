package bastoni.domain.view

import bastoni.domain.model.{PlayerEvent, TablePlayerView}

sealed trait ToPlayer

object ToPlayer:
  case class Snapshot(table: TablePlayerView) extends ToPlayer
  case class GameEvent(event: PlayerEvent) extends ToPlayer
