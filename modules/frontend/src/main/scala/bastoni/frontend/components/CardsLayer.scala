package bastoni.frontend
package components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.Konva
import org.scalajs.dom.window
import reactkonva.KGroup


object CardsLayer:

  def handsLayout(table: TablePlayerView, layout: GameLayout): List[CardsLayout] = {
    val hands: List[Option[List[Option[Card]]]] = List(
      table.mySeat.map(_.hand.map(_.card)),
      table.opponent(0).map(_.hand.map(_.card)),
      table.opponent(1).map(_.hand.map(_.card)),
      table.opponent(2).map(_.hand.map(_.card)),
    )

    val handsLayoutF: List[List[Option[Card]] => CardsLayout] = List(
      layout.player0.hand,
      layout.player1.hand,
      layout.player2.hand,
      layout.player3.hand
    )

    hands.zip(handsLayoutF).flatMap(_ map _)
  }

  def pilesLayout(table: TablePlayerView, layout: GameLayout): List[CardsLayout.Contracted] = {
    val piles = List(
      table.mySeat.map(_.taken.size),
      table.opponent(0).map(_.taken.size),
      table.opponent(1).map(_.taken.size),
      table.opponent(2).map(_.taken.size)
    )

    val pilesPosition = List(
      layout.table.piles.player0,
      layout.table.piles.player1,
      layout.table.piles.player2,
      layout.table.piles.player3,
    )

    piles.zip(pilesPosition).flatMap { case (pile, position) =>
      pile.map(count => CardsLayout.Contracted(position, count, layout.table.piles.sizes))
    }
  }

  private val component =
    ScalaFnComponent[(GameProps, GameLayout)] { case (props: GameProps, layout: GameLayout) =>

      val table = props.table  // transition.map(_._1).getOrElse(props.table)

      val hands = handsLayout(table, layout)
      val piles = pilesLayout(table, layout)
      val deck: List[CardsLayout] = layout.table.deck.cardsLayout(table.deck.map(_.card))
      val board: CardsLayout = CardsLayout.Expanded(layout.table.board.positions(table.board.flatMap(_.card)), layout.table.board.sizes)

      val renderedCards: List[VdomNode] =
        (board :: deck ++ piles ++ hands)
          .flatMap {
            case CardsLayout.Expanded(positions, size, rotation, originalPositions, originalSizes) =>
              Some(KGroup(positions.map {
                case (card, position) => CardComponent(
                  card,
                  size,
                  position,
                  rotation,
                  originalPosition = originalPositions.get(card),
                  originalSize = originalSizes.get(card)
                )
              }: _*))

            case CardsLayout.Contracted(position, count, size, rotation) if count > 0 => Some(CardComponent(count, size, position, rotation))
            case CardsLayout.Contracted(position, count, size, rotation) => None
          }

      KGroup(renderedCards: _*)
    }

  def apply(gameProps: GameProps, gameLayout: GameLayout): VdomNode = component((gameProps, gameLayout))
