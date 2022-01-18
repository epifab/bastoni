package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance}

case class SeatLayout private(
  center: Point,
  radius: Double,
  textRotation: Angle,
  barsRotation: Angle,
  renderHand: CardsRenderer,
  renderPile: CardsRenderer
)

object SeatLayout:
  def apply(
    handRenderer: CardsRenderer,
    seatRadius: Int,
    handSize: CardSize,
    pileSize: CardSize,
    pileOffset: Double,
    center: Point,
    rotation: Angle
  ): SeatLayout = {

    val py1 = pileSize.height - seatRadius

    val yangle = Angle(90 - rotation.deg)
    val xangle = rotation

    new SeatLayout(
      center,
      seatRadius,
      textRotation = rotation.normalised match {
        case a if a > 90 && a <= 270 => Angle(180 - a)
        case a => rotation
      },
      barsRotation = -rotation,
      renderHand = handRenderer,
      renderPile = CardGroupRenderer(
        pileSize,
        Point(
          x = center.x + (xangle.cos * pileOffset) - yangle.cos * py1,
          y = center.y + (xangle.sin * pileOffset) - yangle.sin * py1
        ),
        rotation = -rotation,
        margin = Margin.PerCard(.6)
      )
    )
  }


object MainPlayerHandRenderer:

  val horizontalOverlapFactor = 0.8
  val verticalOverlapFactor = 0.7

  def apply(cardSize: CardSize, canvasSize: Size): CardsRenderer = (hand: List[CardInstance]) =>

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

object OtherPlayersHandRenderer:
  def apply(center: Point, radius: Double, handSize: CardSize, rotation: Angle): CardsRenderer =
    val hy1 = handSize.height - radius - 15
    val yangle = Angle(90 - rotation.deg)

    CardGroupRenderer(
      handSize,
      Point(center.x - yangle.cos * hy1, center.y - yangle.sin * hy1),
      rotation = -rotation,
      margin = Margin.Shared(handSize.width / 4)
    )
