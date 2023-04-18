package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.domain.view.FromPlayer.GameCommand
import bastoni.frontend.model.*
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.ScalaComponent
import reactkonva.{KCircle, KGroup, KLayer, KText}

object DeckShufflingLayer:
  case class Props(room: RoomPlayerView, layout: GameLayout, sendMessage: GameCommand => Callback)
  case class State(mouseOver: Boolean)

  private val component = ScalaComponent
    .builder[Props]
    .initialState(State(mouseOver = false))
    .noBackend
    .renderPS { case (scope, Props(room, layout, sendMessage), State(mouseOver)) =>
      KLayer(
        room.seats
          .map(_.playerOption)
          .collectFirst {
            case Some(PlayerState.Acting(me, Action.ShuffleDeck, _)) if me.is(room.me) =>
              CardComponent(
                layout = layout.deck.controlLayout,
                previous = None,
                eventHandlers = Some(
                  CardEventHandlers(
                    onSelect = sendMessage(FromPlayer.ShuffleDeck),
                    onMouseOver = scope.modState(_.copy(mouseOver = true)),
                    onMouseOut = scope.modState(_.copy(mouseOver = false))
                  )
                ),
                selected = mouseOver
              )

            case Some(PlayerState.Acting(dealer, Action.ShuffleDeck, _)) =>
              KGroup(
                _.opacity = .5,
                CardComponent(
                  layout = layout.deck.controlLayout,
                  previous = None,
                  eventHandlers = None
                ),
                KCircle { p =>
                  val size = layout.deck.controlLayout.size
                  p.radius = size.width / 2
                  p.x = layout.deck.controlLayout.topLeft.x + size.width / 2
                  p.y = layout.deck.controlLayout.topLeft.y + size.height / 2
                  p.fill = Palette.black
                },
                KText { p =>
                  p.text = s"waiting for\n${dealer.name}"
                  p.align = "center"
                  p.verticalAlign = "middle"
                  p.fill = Palette.white
                  p.fontFamily = "'Open Sans', sans-serif"
                  p.fontStyle = "bold"
                  p.fontSize = 28
                  p.x = layout.deck.controlLayout.topLeft.x
                  p.y = layout.deck.controlLayout.topLeft.y
                  p.width = layout.deck.controlLayout.size.width
                  p.height = layout.deck.controlLayout.size.height
                }
              )
          }
          .toList: _*
      )
    }
    .build

  def apply(room: RoomPlayerView, layout: GameLayout, sendMessage: GameCommand => Callback): VdomNode =
    component(Props(room, layout, sendMessage))
end DeckShufflingLayer
