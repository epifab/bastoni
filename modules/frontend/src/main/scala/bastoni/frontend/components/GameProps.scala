package bastoni.frontend.components

import bastoni.domain.model.*

import scala.util.chaining.*

case class GameProps(me: UserId, currentTable: TablePlayerView, previousTable: Option[TablePlayerView])
