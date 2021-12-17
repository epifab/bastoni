package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.domain.view.TableView
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

val TableComponent =
  ScalaComponent
    .builder[TableView]
    .noBackend
    .render_P { table =>
      <.div(^.className := "table",
        table.seats.zipWithIndex
          .map(SeatComponent(_))
          .toTagMod,
        <.p(
          "Deck: ",
          CardsComponent(table.deck)
        )
      )
    }
    .build
