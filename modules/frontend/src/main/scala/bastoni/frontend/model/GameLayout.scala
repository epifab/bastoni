package bastoni.frontend.model

import bastoni.domain.model.{Action, CardInstance, PlayerState, RoomPlayerView}
import org.scalajs.dom.window

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
  private val seatRadius      = 45

  def fromWindow(room: Option[RoomPlayerView]): GameLayout =
    GameLayout(Size(window.innerWidth, window.innerHeight), room)

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

  def apply(canvasSize: Size, room: Option[RoomPlayerView]): GameLayout =
    val pileSize  = CardSize.scaleTo(canvasSize.width / 8, canvasSize.height / 8)
    val deckSize  = pileSize
    val handSize  = CardSize.scaleTo(canvasSize.width / 5, canvasSize.height / 5)
    val boardSize = handSize
    val mainPlayerHandSize: CardSize = room
      .flatMap(_.mainPlayer.flatMap { seat =>
        Some(seat.player).collect {
          case actor: PlayerState.Acting if actor.playing =>
            CardSize.scaleTo(
              maxWidth = canvasSize.width / (1 + (Math
                .min(4, seat.hand.size - 1) * MainPlayerHandRenderer.horizontalOverlapFactor)).floor,
              maxHeight = canvasSize.height / 3
            )
        }
      })
      .getOrElse(handSize)

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
      player3 = otherSeatLayout(
        handSize,
        pileSize,
        Point(canvasSize.width - textHeight, canvasSize.height / 2),
        rotation = Angle(-90)
      ),
      table = TableLayout(Point(0, 0), canvasSize),
      renderBoard = (cards: List[(Option[RoomPlayer], CardInstance)]) =>

        val boardCards: List[CardLayout] =
          CardLayout.group(
            cards.collect { case (None, card) => card },
            boardSize,
            Point(canvasSize.width / 2, canvasSize.height / 2),
            vAlign = Align.Vertical.Middle,
            hAlign = Align.Horizontal.Center,
            cardsPerRow = Some(((canvasSize.width - 100) / (boardSize.width + 4)).floor.toInt)
          )

        val boardCardsByPlayers: List[CardLayout] = cards
          .collect { case (Some(player), card) =>
            CardLayout(
              card,
              boardSize,
              player match
                case RoomPlayer.Player1 =>
                  Point(
                    canvasSize.width / 2 - boardSize.width - cardsMargin,
                    (canvasSize.height - boardSize.height) / 2
                  )
                case RoomPlayer.Player2 =>
                  Point(
                    (canvasSize.width - boardSize.width) / 2,
                    canvasSize.height / 2 - boardSize.height - cardsMargin
                  )
                case RoomPlayer.Player3 =>
                  Point(canvasSize.width / 2 + cardsMargin, (canvasSize.height - boardSize.height) / 2)
                case RoomPlayer.MainPlayer =>
                  Point((canvasSize.width - boardSize.width) / 2, canvasSize.height / 2 + cardsMargin)
              ,
              rotation = Angle.zero,
              shadow = Some(Shadow(4, Point(0, 0)))
            )
          }

        boardCards ++ boardCardsByPlayers
      ,
      deck = DeckLayout(deckSize, canvasSize)
    )
  end apply
end GameLayout
