package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

case class CardProps(card: Card | Int, size: CardSize)

val CardComponent =
  ScalaComponent
    .builder[CardProps]
    .noBackend
    .render_P {
      case CardProps(card: Card, size) =>
        <.span(^.className := s"card ${size.css} face-up ${card.rank.toString.toLowerCase} ${card.suit.toString.toLowerCase}")
      case CardProps(occurrences: Int, size) =>
        <.span(^.className := s"card ${size.css} face-down",
          if (occurrences > 1) <.span(^.className := "occurrences", occurrences)
          else TagMod.empty
        )
    }
    .build

enum CardSize(val css: String):
  case Small extends CardSize("sm")
  case Medium extends CardSize("md")
  case Large extends CardSize("lg")

def CardsComponent(cards: List[CardPlayerView], size: CardSize) =
  def compacted(cx: List[Option[Card]], count: Int): List[Card | Int] =
    cx match
      case None :: tail => compacted(tail, count + 1)
      case Some(card) :: tail if count == 0 => card :: compacted(tail, 0)
      case Some(card) :: tail => count :: card :: compacted(tail, 0)
      case Nil if count > 0 => count :: Nil
      case Nil => Nil

  <.div(^.className := "cards",
    compacted(cards.reverse.map(_.card), 0)
      .map(c => CardComponent(CardProps(c, size)))
      .toTagMod
  )
