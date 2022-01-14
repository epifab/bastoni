package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance, HiddenCard}

object DeckLayout:
  def apply(size: CardSize, topLeft: Point): CardsRenderer =
    (cards: List[CardInstance]) =>
      CardsRenderer.collapseFaceDownCards(cards, Nil)
        .reverse
        .map {
          case hidden: List[HiddenCard] =>
            CardGroupLayout(
              hidden,
              size,
              Point(
                topLeft.x,
                topLeft.y
              ),
              rotation = Angle.zero,
              shadow = Some(Shadow(size.cornerRadius.floor.toInt, .5)),
              marginX = 1
            )

          case card: VisibleCard =>
            CardLayout(
              card,
              size,
              Point(
                topLeft.x + (size.width * .5),
                topLeft.y + (size.height * .8)
              ),
              rotation = Angle(23),
              shadow = None
            )
        }
