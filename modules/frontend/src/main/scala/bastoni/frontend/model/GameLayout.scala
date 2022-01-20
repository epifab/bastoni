package bastoni.frontend.model

import bastoni.domain.model.{Action, CardInstance, PlayerState, TablePlayerView}
import org.scalajs.dom.window

import javax.swing.text.TableView
import scala.util.Random

case class GameLayout(
  canvas: Size,
  mainPlayer: SeatLayout,
  player1: SeatLayout,
  player2: SeatLayout,
  player3: SeatLayout,
  table: TableLayout,
  renderBoard: BoardRenderer,
  deck: DeckLayout
)

object GameLayout:
  private val textHeight: Int = 10
  private val seatRadius = 45

  def fromWindow(table: Option[TablePlayerView]): GameLayout = GameLayout(Size(window.innerWidth, window.innerHeight), table)

  def otherSeatLayout(handSize: CardSize, pileSize: CardSize, center: Point, rotation: Angle): SeatLayout =
    SeatLayout(
      handRenderer = OtherPlayersHandRenderer(center, seatRadius, handSize, rotation),
      seatRadius = seatRadius,
      handSize = handSize,
      pileSize = pileSize,
      pileOffset = handSize.width * 1.2,
      center = center,
      rotation = rotation
    )

  def apply(canvasSize: Size, table: Option[TablePlayerView]): GameLayout = {
    val pileSize = CardSize.scaleTo(canvasSize.width / 8, canvasSize.height / 8)
    val deckSize = pileSize
    val handSize = CardSize.scaleTo(canvasSize.width / 5, canvasSize.height / 5)
    val boardSize = handSize
    val mainPlayerHandSize: CardSize = table.flatMap(_.mainPlayer.flatMap { seat =>
      Some(seat.player).collect {
        case actor: PlayerState.ActingPlayer if actor.playing =>
          CardSize.scaleTo(
            maxWidth = canvasSize.width / (1 + ((seat.hand.size - 1) * MainPlayerHandRenderer.horizontalOverlapFactor)),
            maxHeight = canvasSize.height / 3
          )
      }
    }).getOrElse(handSize)

    val cardsMargin: Int = (boardSize.width / 4).floor.toInt

    GameLayout(
      canvas = canvasSize,
      mainPlayer = SeatLayout(
        handRenderer = MainPlayerHandRenderer(mainPlayerHandSize, canvasSize),
        seatRadius = seatRadius,
        handSize = mainPlayerHandSize,
        pileSize = pileSize,
        pileOffset = canvasSize.width / 3,
        center = Point(canvasSize.width / 2, canvasSize.height - textHeight),
        rotation = Angle(180)
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
                  case None =>
                    // todo: scopa might have N cards on the board, how to display them?
                    Point(
                      (canvasSize.width - boardSize.width) / 2,
                      (canvasSize.height - boardSize.height) / 2
                    ) + Point(Random.nextGaussian(), Random.nextGaussian())
                },
                rotation = Angle.zero,
                shadow = Some(Shadow(3, Point(0, 0)))
              )
          }
      },
      deck = DeckLayout(deckSize, canvasSize)
    )
  }
