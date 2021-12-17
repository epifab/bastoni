package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

val CardComponent =
  ScalaComponent
    .builder[Option[Card]]
    .noBackend
    .render_P {
      case None => <.span(^.className := "card")
      case Some(card) => <.span(^.className := s"card ${card.rank.toString.toLowerCase} ${card.suit.toString.toLowerCase}")
    }
    .build

def CardsComponent(cards: List[CardPlayerView]) =
  <.div(^.className := "cards", cards.map(c => CardComponent(c.card)).toTagMod)
