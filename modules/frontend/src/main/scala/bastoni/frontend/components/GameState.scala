package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer.GameCommand
import japgolly.scalajs.react.callback.Callback

case class GameState(
    currentRoom: RoomPlayerView,
    previousRoom: Option[RoomPlayerView],
    sendMessage: GameCommand => Callback
)
