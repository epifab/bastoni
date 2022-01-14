package bastoni.frontend.model

import bastoni.domain.model.CardInstance

case class GameLayout(
  mainPlayer: SeatLayout,
  player1: SeatLayout,
  player2: SeatLayout,
  player3: SeatLayout,
  table: TableLayout,
  renderBoard: BoardRenderer,
  renderDeck: CardsRenderer
)

object GameLayout:
  private val textHeight: Int = 10
  private val seatRadius = 45

  def apply(canvasSize: Size): GameLayout = {
    val (pileSize, deckSize, boardSize, handSize) =
      if (canvasSize.width > 800 && canvasSize.height > 600) (CardSize.full / 2, CardSize.full / 3 * 2, CardSize.full, CardSize.full / 3 * 2)
      else (CardSize.scaleTo(30), CardSize.scaleTo(seatRadius), CardSize.scaleTo(50), CardSize.scaleTo(seatRadius))

    val cardsMargin: Int = (boardSize.width / 4).floor.toInt

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
      renderBoard = (cards: List[(Option[TablePlayer], CardInstance)]) => {
        cards
          .reverse
          .zipWithIndex
          .map {
            case ((player, card), col) =>
              CardLayout(
                card,
                boardSize,
                player match {
                  case Some(TablePlayer.Player1) =>
                    Point(canvasSize.width / 2 - boardSize.width - cardsMargin, (canvasSize.height - boardSize.height) / 2)
                  case Some(TablePlayer.Player2) =>
                    Point((canvasSize.width - boardSize.width) / 2, canvasSize.height / 2 - boardSize.height - cardsMargin)
                  case Some(TablePlayer.Player3) =>
                    Point(canvasSize.width / 2 + cardsMargin, (canvasSize.height - boardSize.height) / 2)
                  case Some(TablePlayer.MainPlayer) =>
                    Point((canvasSize.width - boardSize.width) / 2, canvasSize.height / 2 + cardsMargin)
                  case None => ???
                },
                rotation = Angle.zero,
                shadow = Some(Shadow(3, Point(0, 0)))
              )
          }
      },
      renderDeck = DeckLayout(deckSize, Point(cardsMargin, cardsMargin))
    )
  }
