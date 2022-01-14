package bastoni.frontend.model

import bastoni.domain.model.CardInstance

case class GameLayout(
  mainPlayer: SeatLayout,
  player1: SeatLayout,
  player2: SeatLayout,
  player3: SeatLayout,
  table: TableLayout,
  renderBoard: CardsRenderer,
  renderDeck: CardsRenderer
)

object GameLayout:
  private val cardsMargin: Int = 10
  private val textHeight: Int = 10
  val seatRadius = 45

  def apply(canvasSize: Size): GameLayout = {
    val (pileSize, deckSize, boardSize, handSize) =
      if (canvasSize.width > 800 && canvasSize.height > 600) (CardSize.full / 2, CardSize.full / 3 * 2, CardSize.full, CardSize.full / 3 * 2)
      else (CardSize.scaleTo(30), CardSize.scaleTo(seatRadius), CardSize.scaleTo(50), CardSize.scaleTo(seatRadius))

    val topLeftTable = Point(0, handSize.height + 70)
    val bottomRightTable = Point(canvasSize.width, canvasSize.height - MainPlayerHandRenderer.cardSizeFor(canvasSize).height - 2 * cardsMargin)
    val middleTable = topLeftTable.y + ((bottomRightTable.y - topLeftTable.y) / 2)
    val tableSize = Size(bottomRightTable.x - topLeftTable.x, bottomRightTable.y - topLeftTable.y)

    GameLayout(
      mainPlayer = SeatLayout(
        center = Point(canvasSize.width / 2, canvasSize.height - textHeight),
        rotation = Angle.zero,
        radius = seatRadius,
        renderHand = MainPlayerHandRenderer(canvasSize),
        renderPile = CardGroupRenderer(
          pileSize,
          center = Point(canvasSize.width / 2, canvasSize.height - textHeight - pileSize.height),
          rotation = Angle.zero,
          margin = .6
        )
      ),

      player1 = OtherSeatLayout(handSize, pileSize, Point(textHeight, canvasSize.height / 2), rotation = Angle(90)),
      player2 = OtherSeatLayout(handSize, pileSize, Point(canvasSize.width / 2, textHeight), rotation = Angle.zero),
      player3 = OtherSeatLayout(handSize, pileSize, Point(canvasSize.width - textHeight, canvasSize.height / 2), rotation = Angle(-90)),

      table = TableLayout(Point(0, 0), canvasSize),  // topLeftTable, tableSize),
      renderBoard = (cards: List[CardInstance]) => {
        cards
          .reverse
          .zipWithIndex
          .map { case (card, col) =>
            CardLayout(
              card,
              boardSize,
              Point(
                deckSize.width * 1.9 + (2 * cardsMargin) + ((boardSize.width + cardsMargin) * col),
                middleTable - (deckSize.height / 2)
              ),
              rotation = Angle.zero,
              shadow = None
            )
          }
      },
      renderDeck = DeckLayout(deckSize, Point(cardsMargin, cardsMargin))
    )
  }
