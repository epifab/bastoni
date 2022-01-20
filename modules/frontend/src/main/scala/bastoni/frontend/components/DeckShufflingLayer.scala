package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.frontend.model.*
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.vdom.VdomNode
import reactkonva.{KCircle, KGroup, KLayer, KText}

object DeckShufflingLayer:
  case class Props(table: TablePlayerView, layout: GameLayout, callback: FromPlayer => Callback)

  private val component = ScalaFn[Props] { case Props(table, layout, callback) =>
    KLayer(
      table.seats.collectFirst {
        case Seat(Some(PlayerState.ActingPlayer(me, Action.ShuffleDeck, _)), _, _) if me.is(table.me) =>
          CardComponent(
            layout = layout.deck.controlLayout,
            previous = None,
            selectable = Some(callback(FromPlayer.ShuffleDeck))
          )

        case Seat(Some(PlayerState.ActingPlayer(dealer, Action.ShuffleDeck, _)), _, _) =>
          KGroup(
            _.opacity = .5,
            CardComponent(
              layout = layout.deck.controlLayout,
              previous = None,
              selectable = None
            ),
            KCircle { p =>
              val size = layout.deck.controlLayout.size
              p.radius = size.width / 2
              p.x = layout.deck.controlLayout.position.x + size.width / 2
              p.y = layout.deck.controlLayout.position.y + size.height / 2
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
              p.x = layout.deck.controlLayout.position.x
              p.y = layout.deck.controlLayout.position.y
              p.width = layout.deck.controlLayout.size.width
              p.height = layout.deck.controlLayout.size.height
            }
          )
      }.toList: _*
    )
  }

  def apply(table: TablePlayerView, layout: GameLayout, callback: FromPlayer => Callback): VdomNode =
    component(Props(table, layout, callback))
