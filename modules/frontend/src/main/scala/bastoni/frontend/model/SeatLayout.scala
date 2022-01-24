package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance}

case class SeatLayout private(
  center: Point,
  radius: Double,
  textRotation: Angle,
  rotation: Angle,
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
      rotation = rotation,
      renderHand = handRenderer,
      renderPile = (pile: List[CardInstance]) => CardLayout.group(
        pile,
        pileSize,
        position = center +
          Point(yangle.cos * 15, yangle.sin * 15) +
          Point(yangle.sin * pileOffset, -yangle.cos * pileOffset),
        rotation = -rotation,
        hMargin = Margin.PerCard(.6),
        vAlign = Align.Vertical.Bottom
      )
    )
  }


object MainPlayerHandRenderer:

  val horizontalOverlapFactor = 0.8
  val verticalOverlapFactor = 0.7

  def apply(cardSize: CardSize, canvasSize: Size): CardsRenderer = (hand: List[CardInstance]) =>

    val verticalOverlap: Double = cardSize.height * verticalOverlapFactor

    val cardsPerRow: Int = ((canvasSize.width - cardSize.width) / (cardSize.width * horizontalOverlapFactor)).floor.toInt + 1

    CardLayout.group(
      hand,
      cardSize,
      Point(
        canvasSize.width / 2,
        canvasSize.height
      ),
      vAlign = Align.Vertical.Bottom,
      hAlign = Align.Horizontal.Center,
      hMargin = Margin.PerCard(cardSize.width * horizontalOverlapFactor),
      vMargin = Margin.PerCard(cardSize.height * verticalOverlapFactor),
      shadow = Some(Shadow(10, Point(-10, 0))),
      cardsPerRow = Some(cardsPerRow)
    )

object OtherPlayersHandRenderer:
  def apply(center: Point, radius: Double, handSize: CardSize, rotation: Angle): CardsRenderer =
    val yangle = Angle(90 - rotation.deg)

    (hand: List[CardInstance]) =>
      CardLayout.group(
        cards = hand,
        size = handSize,
        position = Point(
          center.x + yangle.cos * (radius + 10),
          center.y + yangle.sin * (radius + 10)
        ),
        rotation = -rotation,
        vAlign = Align.Vertical.Bottom,
        hAlign = Align.Horizontal.Center,
        hMargin = Margin.Total(handSize.width / 2),
        shadow = Some(Shadow(4, Point(-4, 0)))
      )
