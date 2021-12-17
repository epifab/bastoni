package bastoni.domain.view

import bastoni.domain.model.*

sealed trait ToPlayer

object ToPlayer:
  case class Snapshot(table: TableView) extends ToPlayer
