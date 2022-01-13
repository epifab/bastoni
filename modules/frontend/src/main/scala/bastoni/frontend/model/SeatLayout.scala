package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance}

case class SeatLayout(
  center: Point,
  radius: Double,
  renderHand: CardsRenderer,
  renderPile: CardsRenderer
)

object OtherSeatLayout:
  def apply(handSize: CardSize, pileSize: CardSize, center: Point): SeatLayout =
    SeatLayout(
      center,
      radius = 45,
      renderHand = CardGroupRenderer(
        handSize,
        center.x,
        center.y + 20,
        cards => if (cards.isEmpty) 0 else 30 / cards.length
      ),
      renderPile = CardGroupRenderer(pileSize, center.x, center.y + 20 + handSize.height + 20)
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
              rotation = 0,
              shadow = Some(Shadow(10, .8))
            )
          }
      }
      .toList


