package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance, HiddenCard}

object DeckLayout:
  def apply(sizes: CardSize, topLeft: Point): CardsRenderer =
    (cards: List[CardInstance]) =>
      CardsRenderer.collapseFaceDownCards(cards, Nil)
        .reverse
        .map {
          case hidden: List[HiddenCard] =>
            CardGroupLayout(
              hidden,
              sizes,
              Point(
                topLeft.x,
                topLeft.y
              ),
              rotation = 0,
              shadowSize = 0,
              margin = .5
            )

          case card: VisibleCard =>
            CardLayout(
              card,
              sizes,
              Point(
                topLeft.x + (sizes.width * .5),
                topLeft.y + (sizes.height * .8)
              ),
              rotation = 23,
              shadowSize = 0
            )
        }
