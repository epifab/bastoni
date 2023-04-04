package bastoni.frontend.model

import bastoni.domain.model.{CardInstance, HiddenCard, UserId, VisibleCard}

case class CardLayout(
    card: CardInstance,
    size: CardSize,
    topLeft: Point,
    rotation: Angle,
    shadow: Option[Shadow]
)

object CardLayout:
  def group(
      cards: List[CardInstance],
      size: CardSize,
      position: Point,
      hMargin: Margin = Margin.Default,
      vMargin: Margin = Margin.Default,
      rotation: Angle = Angle.zero,
      shadow: Option[Shadow] = None,
      hAlign: Align.Horizontal = Align.Horizontal.Left,
      vAlign: Align.Vertical = Align.Vertical.Top,
      cardsPerRow: Option[Int] = None
  ): List[CardLayout] =

    val rows: List[List[CardInstance]] = cardsPerRow.fold(List(cards))(cards.grouped(_).toList)

    val vSize: Double = vMargin.total(size.height, rows.length) + size.height
    val vOffset = vAlign match
      case Align.Vertical.Top    => Point(0, 0)
      case Align.Vertical.Bottom => Point(-rotation.sin * vSize, rotation.cos * vSize)
      case Align.Vertical.Middle => Point(-rotation.sin * vSize / 2, rotation.cos * vSize / 2)

    rows.zipWithIndex.flatMap { case (cols, rowIndex) =>
      val top = Point(
        -rotation.sin * rowIndex * vMargin.perCard(size.height, rows.length),
        rotation.cos * rowIndex * vMargin.perCard(size.height, rows.length)
      ) - vOffset

      val hSize: Double = hMargin.total(size.width, cols.length) + size.width
      val hOffset = hAlign match
        case Align.Horizontal.Left   => Point(0, 0)
        case Align.Horizontal.Right  => Point(rotation.cos * hSize, rotation.sin * hSize)
        case Align.Horizontal.Center => Point(rotation.cos * hSize / 2, rotation.sin * hSize / 2)

      cols.zipWithIndex.map { case (card, colIndex) =>
        val left = Point(
          rotation.cos * colIndex * hMargin.perCard(size.width, cols.length),
          rotation.sin * colIndex * hMargin.perCard(size.width, cols.length)
        ) - hOffset

        CardLayout(
          card,
          size,
          topLeft = position + top + left,
          rotation = rotation,
          shadow = shadow.map(s => s.copy(offset = s.offset.rotate(rotation)))
        )
      }
    }
  end group
end CardLayout
