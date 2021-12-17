package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.domain.view.SeatView
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

val SeatComponent =
  ScalaComponent
    .builder[(SeatView, Int)]
    .noBackend
    .render_P { case (seat, index) =>
      <.div(^.className := s"seat seat-$index",
        PlayerComponent(seat.player),
        <.p(
          "Hand: ",
          CardsComponent(seat.hand)
        ),
        <.p(
          "Collected: ",
          CardsComponent(seat.collected)
        ),
        <.p(
          "Played: ",
          CardsComponent(seat.played)
        ),
        <.hr
      )
    }
    .build
