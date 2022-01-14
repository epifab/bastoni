package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance}

case class SeatLayout(
  center: Point,
  radius: Double,
  rotation: Angle,
  renderHand: CardsRenderer,
  renderPile: CardsRenderer
)

object OtherSeatLayout:
  def apply(handSize: CardSize, pileSize: CardSize, center: Point, rotation: Angle): SeatLayout =
    SeatLayout(
      center,
      radius = 45,
      rotation,
      renderHand = CardGroupRenderer(
        handSize,
        center,
        Angle(-rotation.deg),
        margin = 30
      ),
      renderPile = CardGroupRenderer(
        pileSize,
        center,
        Angle(-rotation.deg),
        margin = .6
      )
    )


object MainPlayerHandRenderer:
  def cardSizeFor(canvasSize: Size): CardSize = {
    val defaultSize: CardSize = CardSize.full
    defaultSize.scaleTo(
      Math.min(
        defaultSize.width * 2,
        (canvasSize.width / ((maxCardsPerRow - 1) * horizontalOverlapFactor) + 1).floor
      )
    )
  }

  val horizontalOverlapFactor = 0.8
  val verticalOverlapFactor = 0.7

  val maxCardsPerRow: Int = 5

  def apply(canvasSize: Size): CardsRenderer = (hand: List[CardInstance]) =>
    val cardSize = cardSizeFor(canvasSize)

    val horizontalOverlap: Double = cardSize.width * horizontalOverlapFactor
    val verticalOverlap: Double = cardSize.height * verticalOverlapFactor

    val cardsPerRow: Int = ((canvasSize.width - cardSize.width) / horizontalOverlap).floor.toInt + 1
    val numberOfRows: Int = (hand.size / cardsPerRow.toDouble).ceil.toInt
    val verticalOffset: Double = canvasSize.height - cardSize.height - ((numberOfRows - 1) * verticalOverlap)

    hand
      .grouped(cardsPerRow)
      .zipWithIndex
      .flatMap { case (cards, row) =>
        val rowSize: Double = cardSize.width + (horizontalOverlap * (cards.size - 1))
        val horizontalOffset: Double = Math.max(0, canvasSize.width - rowSize) / 2.0
        cards.zipWithIndex
          .map { case (card, col) =>
            CardLayout(
              card,
              cardSize,
              Point(
                horizontalOffset + (horizontalOverlap * col),
                verticalOffset + (verticalOverlap * row)
              ),
              rotation = Angle.zero,
              shadow = Some(Shadow(8, Point(-6, 0)))
            )
          }
      }
      .toList


