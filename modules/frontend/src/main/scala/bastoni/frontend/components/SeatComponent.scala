package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

val SeatComponent =
  ScalaComponent
    .builder[(Seat[CardPlayerView], Int)]
    .noBackend
    .render_P { case (seat, index) =>
      <.div(^.className := s"seat seat-$index",
        PlayerComponent(seat.player),
        <.div(^.className := "hand",
          CardsComponent(seat.hand, CardSize.Medium)
        ),
        <.div(^.className := "taken",
          CardsComponent(seat.taken, CardSize.Small)
        )
      )
    }
    .build
