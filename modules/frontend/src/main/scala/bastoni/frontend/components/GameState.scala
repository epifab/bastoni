package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import japgolly.scalajs.react.callback.Callback

case class GameState(
    currentRoom: RoomPlayerView,
    previousRoom: Option[RoomPlayerView],
    sendMessage: FromPlayer => Callback
)
