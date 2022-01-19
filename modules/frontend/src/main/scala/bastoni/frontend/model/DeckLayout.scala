package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance, HiddenCard}

case class DeckLayout(renderCards: CardsRenderer, topLeft: Point, radius: Double, controlLayout: CardLayout)

object DeckLayout:
  def apply(size: CardSize, canvas: Size): DeckLayout =
    val rotation = Angle(45)
    val topLeft = Point(rotation.cos * size.height / 2, -rotation.sin * size.height / 2)

    val renderer: List[CardInstance] => List[CardLayout | CardGroupLayout] = (cards: List[CardInstance]) =>
      CardsRenderer.collapseFaceDownCards(cards, Nil)
        .reverse
        .map {
          case hidden: List[HiddenCard] =>
            CardGroupLayout(
              hidden,
              size,
              topLeft,
              rotation = rotation,
              shadow = Some(Shadow(size.cornerRadius.floor.toInt, Point(-size.cornerRadius, 0))),
              margin = Margin.PerCard(.5)
            )

          case card: VisibleCard =>
            CardLayout(
              card,
              size,
              Point(
                topLeft.x + rotation.cos * size.width,
                topLeft.y + rotation.sin * size.width
              ),
              rotation = rotation,
              shadow = Some(Shadow(3, Point(0, 0)))
            )
        }

    val controlSize = CardSize.scaleTo(canvas.width / 2, canvas.height / 2)

    new DeckLayout(
      renderer,
      Point(size.width / 2, size.width / 2),
      size.width / 2,
      controlLayout = CardLayout(
        HiddenCard(0),
        controlSize,
        Point(
          (canvas.width - controlSize.width) / 2,
          (canvas.height - controlSize.height) / 2
        ),
        Angle.zero,
        None
      )
    )