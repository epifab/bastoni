package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

val CardComponent =
  ScalaComponent
    .builder[Option[Card]]
    .noBackend
    .render_P {
      case None => <.span(^.className := "card back", "[?]")
      case Some(card) => <.span(^.className := s"card rank-${card.rank} suit-${card.rank}", s"[${card.rank} ${card.suit}]")
    }
    .build

def CardsComponent(cards: List[Option[Card]]) =
  if (cards.isEmpty) <.span("No cards") else cards.map(CardComponent(_)).toTagMod
