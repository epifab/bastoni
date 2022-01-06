package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.VdomNode
import konva.Konva
import org.scalajs.dom.window
import reactkonva.KGroup


object CardsLayer:
  private def faceDownCompacted(cx: List[Option[Card]], count: Int): List[Card | Int] =
    cx match
      case None :: tail => faceDownCompacted(tail, count + 1)
      case Some(card) :: tail if count == 0 => card :: faceDownCompacted(tail, 0)
      case Some(card) :: tail => count :: card :: faceDownCompacted(tail, 0)
      case Nil if count > 0 => count :: Nil
      case Nil => Nil

  private val component =
    ScalaFnComponent[GameProps] { props =>
      val containerWidth: Double = window.innerWidth
      val containerHeight: Double = window.innerHeight

      val cardOffsetFactorX = 0.8
      val cardOffsetFactorY = 0.7

      val playerCardsPerRow: Int = 5
      val fullCardSize: CardSize = ScaledCardSize.width(Math.min(FullCardSize.width, (containerWidth / ((playerCardsPerRow - 1) * cardOffsetFactorX) + 1).floor))
      val mediumCardSize: CardSize = ScaledCardSize(fullCardSize, 0.5)
      val smallCardSize: CardSize = ScaledCardSize(fullCardSize, 0.3)

      val cardOffsetX: Double = fullCardSize.width * cardOffsetFactorX
      val cardOffsetY: Double = fullCardSize.height * cardOffsetFactorY

      val deckOffsetX = 0
      val deckOffsetY = (containerHeight - mediumCardSize.height) / 2

      val deck: VdomNode =
        KGroup
          .build(
            faceDownCompacted(props.table.deck.map(_.card), 0)
              .zipWithIndex
              .reverse
              .map { case (card, col) =>
                KGroup
                  .builder
                  .set(_.y = deckOffsetY)
                  .set(_.x = deckOffsetX + (col * mediumCardSize.width * cardOffsetFactorX))
                  .build(CardComponent(card, mediumCardSize))
              }: _*
          )

      val boardOffsetX = mediumCardSize.width * 3
      val boardOffsetY = FullCardSize.height
      val boardCardsPerRow = (containerWidth / (mediumCardSize.width + 2)).floor.toInt
      val board =
        KGroup
          .build(
            faceDownCompacted(props.table.board.map(_.card), 0)
              .reverse
              .grouped(boardCardsPerRow)
              .zipWithIndex
              .flatMap { case (line, row) =>
                line.zipWithIndex.map { case (card, col) =>
                  KGroup
                    .builder
                    .set(_.x = boardOffsetX + ((mediumCardSize.width + 2) * col))
                    .set(_.y = boardOffsetY + ((mediumCardSize.height + 2) * row))
                    .build(CardComponent(card, mediumCardSize))
                }
              }.toSeq: _*
          )


      val myCards = props.table.seatFor(props.me) match {
        case Some(TakenSeat(me, hand, taken)) =>
          val cardsPerRow: Int = ((containerWidth - fullCardSize.width) / cardOffsetX).floor.toInt + 1
          val numberOfRows: Int = (hand.size / cardsPerRow.toDouble).ceil.toInt
          val verticalOffset: Double = containerHeight - fullCardSize.height - ((numberOfRows - 1) * cardOffsetY)

          KGroup
            .builder
            .build(
              faceDownCompacted(hand.map(_.card), 0)
                .grouped(cardsPerRow)
                .zipWithIndex
                .flatMap { case (cards, row) =>
                  val rowSize: Double = fullCardSize.width + (cardOffsetX * (cards.size - 1))
                  val horizontalOffset: Double = Math.max(0, containerWidth - rowSize) / 2.0
                  cards.zipWithIndex
                    .map { case (card, col) =>
                      KGroup
                        .builder
                        .set(_.width = containerWidth)
                        .set(_.height = containerHeight)
                        .set(_.x = horizontalOffset + (cardOffsetX * col))
                        .set(_.y = verticalOffset + (row * cardOffsetY))
                        .build(CardComponent(card, fullCardSize))
                    }
                }.toSeq: _*
            )

        case None => KGroup.build()
      }

      KGroup.build(deck, board, myCards)
    }

  def apply(gameProps: GameProps): VdomNode = component(gameProps)
