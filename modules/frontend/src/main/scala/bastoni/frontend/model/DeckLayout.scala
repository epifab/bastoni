package bastoni.frontend.model

import bastoni.domain.model.{CardId, CardInstance, HiddenCard, VisibleCard}

case class DeckLayout(renderCards: CardsRenderer, topLeft: Point, radius: Double, controlLayout: CardLayout)

object DeckLayout:
  def apply(size: CardSize, canvas: Size): DeckLayout =
    val rotation = Angle(45)
    val renderer: List[CardInstance] => List[CardLayout] = (cards: List[CardInstance]) =>
      CardsRenderer
        .collapseFaceDownCards(cards, Nil)
        .reverse
        .flatMap {
          case hidden: List[HiddenCard] =>
            CardLayout.group(
              hidden,
              size,
              Point(rotation.cos * size.height / 2, -rotation.sin * size.height / 2),
              rotation = rotation,
              shadow = Some(Shadow(size.cornerRadius.floor.toInt, Point(-size.cornerRadius, 0))),
              hMargin = Margin.PerCard(.5)
            )

          case card: VisibleCard =>
            List(
              CardLayout(
                card,
                size,
                Point(
                  Point(rotation.cos * size.height / 2, -rotation.sin * size.height / 2).x + rotation.cos * size.width,
                  Point(rotation.cos * size.height / 2, -rotation.sin * size.height / 2).y + rotation.sin * size.width
                ),
                rotation = rotation,
                shadow = Some(Shadow(3, Point(0, 0)))
              )
            )
        }

    val controlSize = CardSize.scaleTo(canvas.width / 2, canvas.height / 2)

    new DeckLayout(
      renderer,
      Point(size.width / 2, size.width / 2),
      size.width / 2,
      controlLayout = CardLayout(
        HiddenCard(CardId.unknown),
        controlSize,
        Point(
          (canvas.width - controlSize.width) / 2,
          (canvas.height - controlSize.height) / 2
        ),
        Angle.zero,
        None
      )
    )
  end apply
end DeckLayout
