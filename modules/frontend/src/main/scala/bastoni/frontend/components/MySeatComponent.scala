package bastoni.frontend.components

import bastoni.domain.model.{CardPlayerView, Seat, TakenSeat}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.vdom.html_<^.*

val MySeatComponent =
  ScalaComponent
    .builder[Option[TakenSeat[CardPlayerView]]]
    .noBackend
    .render_P { case seat =>
      <.div(^.className := s"my-seat",
        PlayerComponent(seat.map(_.player)),
        seat.whenDefined { seat =>
          TagMod(
            <.div(^.className := "hand",
              CardsComponent(seat.hand, CardSize.Large)
            ),
            <.div(^.className := "taken",
              CardsComponent(seat.taken, CardSize.Medium)
            )
          )
        }
      )
    }
    .build
