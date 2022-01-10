package bastoni.frontend.components

import bastoni.domain.model.*

import scala.util.chaining.*

case class GameProps(table: TablePlayerView, me: UserId, transition: Option[(TablePlayerView, PlayerEvent)])