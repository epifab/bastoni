package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.util.chaining.*

val TableComponent =
  ScalaComponent
    .builder[GameProps]
    .noBackend
    .render_P { props =>
      <.main(
        <.div(^.className := "opponents",
          props.opponents
            .map(SeatComponent(_))
            .toTagMod
        ),
        <.div(^.className := "table",
          <.div(^.className := "deck",
            CardsComponent(props.table.deck, CardSize.Medium)
          ),
          <.div(^.className := "board",
            CardsComponent(props.table.board, CardSize.Medium)
          )
        ),
        MySeatComponent(props.mySeat),
      )
    }
    .build
