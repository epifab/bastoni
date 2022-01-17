package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import japgolly.scalajs.react.callback.Callback

case class GameState(
  me: UserId,
  currentTable: TablePlayerView,
  previousTable: Option[TablePlayerView],
  callback: FromPlayer => Callback
)