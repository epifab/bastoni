package bastoni.domain.view

import bastoni.domain.model.PlayerTableView

sealed trait ToPlayer

object ToPlayer:
  case class Snapshot(table: PlayerTableView) extends ToPlayer
