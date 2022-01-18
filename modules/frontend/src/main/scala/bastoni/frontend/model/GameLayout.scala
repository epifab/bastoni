package bastoni.frontend.model

import bastoni.domain.model.{Action, CardInstance, PlayerState, TablePlayerView}
import org.scalajs.dom.window

import javax.swing.text.TableView

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

  def fromWindow(table: Option[TablePlayerView]): GameLayout = GameLayout(Size(window.innerWidth, window.innerHeight), table)

  def otherSeatLayout(handSize: CardSize, pileSize: CardSize, center: Point, rotation: Angle): SeatLayout =
    SeatLayout(
      OtherPlayersHandRenderer(center, seatRadius, handSize, rotation),
      seatRadius,
      handSize,
      pileSize,
      center,
      rotation
    )

  def apply(canvasSize: Size, table: Option[TablePlayerView]): GameLayout = {
    val pileSize = CardSize.scaleTo(Math.min(canvasSize.width / 8, canvasSize.height / 8 * CardSize.ratioW))
    val deckSize = CardSize.scaleTo(Math.min(canvasSize.width / 8, canvasSize.height / 8 * CardSize.ratioW))
    val handSize = CardSize.scaleTo(Math.min(canvasSize.width / 5, canvasSize.height / 5 * CardSize.ratioW))
    val boardSize = CardSize.scaleTo(Math.min(canvasSize.width / 5, canvasSize.height / 5 * CardSize.ratioW))
    val mainPlayerHandSize: CardSize = table.flatMap(_.mySeat.flatMap { seat =>
      Some(seat.player).collect {
        case actor: PlayerState.ActingPlayer if actor.playing =>
          CardSize.scaleTo(
            Math.min(
              (canvasSize.height / 3) * CardSize.ratioW,
              canvasSize.width / (1 + ((seat.hand.size - 1) * MainPlayerHandRenderer.horizontalOverlapFactor))
            )
          )
      }
    }).getOrElse(handSize)

    val cardsMargin: Int = (boardSize.width / 4).floor.toInt

    GameLayout(
      mainPlayer = SeatLayout(
        MainPlayerHandRenderer(mainPlayerHandSize, canvasSize),
        seatRadius,
        mainPlayerHandSize,
        pileSize,
        center = Point(canvasSize.width / 2, canvasSize.height - textHeight),
        Angle(180)
      ),

      player1 = otherSeatLayout(handSize, pileSize, Point(textHeight, canvasSize.height / 2), rotation = Angle(90)),
      player2 = otherSeatLayout(handSize, pileSize, Point(canvasSize.width / 2, textHeight), rotation = Angle.zero),
      player3 = otherSeatLayout(handSize, pileSize, Point(canvasSize.width - textHeight, canvasSize.height / 2), rotation = Angle(-90)),

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
