package bastoni.frontend

import bastoni.domain.model.*

case class Point(x: Double, y: Double)

case class Size(width: Double, height: Double):
  def scale(factor: Double): Size = Size(width * factor, height * factor)
  def scaleTo(newWidth: Double): Size = scale(newWidth / width)

extension (t: (Double, Double))
  def toPoint: Point = Point(t._1, t._2)
  def toSize: Size = Size(t._1, t._2)

case class CardSize(size: Size, borderRadius: Double):
  def width: Double = size.width
  def height: Double = size.height
  def scale(factor: Double): CardSize = CardSize(size.scale(factor), borderRadius * factor)
  def scaleTo(newWidth: Double): CardSize = scale(newWidth / size.width)

object CardSize:
  val full: CardSize = CardSize(Size(90, 148), borderRadius = 10)

case class PilesLayout(
  player0: Point,
  player1: Point,
  player2: Point,
  player3: Point,
  sizes: CardSize
)

case class DeckLayout(sizes: CardSize, position: Point)

case class BoardLayout(
  sizes: CardSize,
  positions: List[Card] => List[(Card, Point)]
)

case class TableLayout(
  position: Point,
  size: Size,
  piles: PilesLayout,
  deck: DeckLayout,
  board: BoardLayout,
)

object TableLayout:
  val pileSize: CardSize = CardSize.full.scaleTo(30)
  val deckSize: CardSize = CardSize.full.scaleTo(45)
  val boardSize: CardSize = deckSize

  def apply(canvasSize: Size): TableLayout = {
    val topLeftTable = Point(0, OtherPlayerLayout.cardSize.height + 80)
    val bottomRightTable = Point(canvasSize.width, canvasSize.height - Player0HandLayout.defaultSize.height * 1.9)
    val middleTable = topLeftTable.y + ((bottomRightTable.y - topLeftTable.y) / 2)
    val tableSize = Size(bottomRightTable.x - topLeftTable.x, bottomRightTable.y - topLeftTable.y)
    val cardsMargin = 10

    TableLayout(
      topLeftTable,
      tableSize,
      PilesLayout(
        player0 = Point(
          canvasSize.width / 2 - (pileSize.width / 2),
          bottomRightTable.y - pileSize.height - 10
        ),
        player1 = Point(
          canvasSize.width / 6 - (pileSize.width / 2),
          topLeftTable.y + cardsMargin
        ),
        player2 = Point(
          canvasSize.width / 2 - (pileSize.width / 2),
          topLeftTable.y + cardsMargin
        ),
        player3 = Point(
          canvasSize.width * 5 / 6 - (pileSize.width / 2),
          topLeftTable.y + cardsMargin
        ),
        pileSize
      ),
      DeckLayout(
        deckSize,
        Point(cardsMargin, middleTable + (deckSize.height / 2))
      ),
      BoardLayout(
        sizes = boardSize,
        cards => {
          cards
            .reverse
            .zipWithIndex
            .map { case (card, col) =>
              card -> Point(
                deckSize.width * 1.9 + (2 * cardsMargin) + ((boardSize.width + cardsMargin) * col),
                middleTable - (deckSize.height / 2)
              )
            }
        }
      )
    )
  }


sealed trait CardsLayout

object CardsLayout:
  case class Contracted(position: Point, count: Int, sizes: CardSize) extends CardsLayout
  case class Expanded(positions: List[(Card, Point)], sizes: CardSize) extends CardsLayout

object Player0HandLayout:
  val defaultSize: CardSize = CardSize.full

  val horizontalOverlapFactor = 0.8
  val verticalOverlapFactor = 0.7

  val maxCardsPerRow: Int = 5

  def apply(canvasSize: Size, hand: List[Card]): CardsLayout = {
    val cardSize: CardSize = defaultSize.scaleTo(
      Math.min(
        defaultSize.width,
        (canvasSize.width / ((maxCardsPerRow - 1) * horizontalOverlapFactor) + 1).floor
      )
    )

    val horizontalOverlap: Double = cardSize.width * horizontalOverlapFactor
    val verticalOverlap: Double = cardSize.height * verticalOverlapFactor

    val cardsPerRow: Int = ((canvasSize.width - cardSize.width) / horizontalOverlap).floor.toInt + 1
    val numberOfRows: Int = (hand.size / cardsPerRow.toDouble).ceil.toInt
    val verticalOffset: Double = canvasSize.height - cardSize.height - ((numberOfRows - 1) * verticalOverlap)

    val positions: List[(Card, Point)] = hand
      .grouped(cardsPerRow)
      .zipWithIndex
      .flatMap { case (cards, row) =>
        val rowSize: Double = cardSize.width + (horizontalOverlap * (cards.size - 1))
        val horizontalOffset: Double = Math.max(0, canvasSize.width - rowSize) / 2.0
        cards.zipWithIndex
          .map { case (card, col) =>
            card -> Point(
              horizontalOffset + (horizontalOverlap * col),
              verticalOffset + (verticalOverlap * row)
            )
          }
      }
      .toList

    CardsLayout.Expanded(positions, cardSize)
  }

object OtherPlayerLayout:
  val cardSize: CardSize = CardSize.full.scaleTo(50)

  def apply(center: Point): PlayerLayout =
    PlayerLayout(
      position = center,
      radius = 45,
      hand = (cards: List[Option[Card]]) => CardsLayout.Contracted(
        position = Point(
          center.x - (cardSize.width / 2),
          center.y + 20
        ),
        cards.size,
        cardSize
      )
    )

case class PlayerLayout(
  position: Point,
  radius: Double,
  hand: List[Option[Card]] => CardsLayout
)

case class GameLayout(
  player0: PlayerLayout,
  player1: PlayerLayout,
  player2: PlayerLayout,
  player3: PlayerLayout,
  table: TableLayout
)

object GameLayout:
  def apply(canvasSize: Size): GameLayout =
    GameLayout(
      player0 = PlayerLayout(
        position = Point(canvasSize.width / 2, canvasSize.height),
        radius = 90,
        hand = (cards: List[Option[Card]]) => Player0HandLayout(canvasSize, cards.flatten)
      ),
      player1 = OtherPlayerLayout(Point(canvasSize.width / 6, 50)),
      player2 = OtherPlayerLayout(Point(canvasSize.width / 2, 50)),
      player3 = OtherPlayerLayout(Point(canvasSize.width * 5 / 6, 50)),
      TableLayout(canvasSize)
    )
