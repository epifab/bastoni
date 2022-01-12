package bastoni.frontend.model

import bastoni.domain.model.CardInstance

object CardGroupRenderer:
  def apply(size: CardSize, middle: Double, top: Double, margin: List[CardInstance] => Double = _ => .6): CardsRenderer =
    (cards: List[CardInstance]) =>
      List(CardGroupLayout(
        cards,
        size,
        Point(
          middle - (size.width / 2),
          top
        ),
        rotation = 0,
        shadowSize = 0,
        margin = margin(cards)
      ))
