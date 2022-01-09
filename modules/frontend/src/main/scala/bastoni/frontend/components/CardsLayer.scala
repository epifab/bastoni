package bastoni.frontend
package components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.Konva
import org.scalajs.dom.window
import reactkonva.KGroup


object CardsLayer:
  private def compactFaceDownCards(cx: List[Option[Card]], count: Int): List[Card | Int] =
    cx match
      case None :: tail => compactFaceDownCards(tail, count + 1)
      case Some(card) :: tail if count == 0 => card :: compactFaceDownCards(tail, 0)
      case Some(card) :: tail => count :: card :: compactFaceDownCards(tail, 0)
      case Nil if count > 0 => count :: Nil
      case Nil => Nil

  private val component =
    ScalaFnComponent[(GameProps, GameLayout)] { case (props: GameProps, layout: GameLayout) =>

      // ----------------------
      // hands
      // ----------------------

      val hands: List[Option[List[Option[Card]]]] = List(
        props.mySeat.map(_.hand.map(_.card)),
        props.opponent(0).map(_.hand.map(_.card)),
        props.opponent(1).map(_.hand.map(_.card)),
        props.opponent(2).map(_.hand.map(_.card)),
      )

      val handsLayoutF: List[List[Option[Card]] => CardsLayout] = List(
        layout.player0.hand,
        layout.player1.hand,
        layout.player2.hand,
        layout.player3.hand
      )

      val handsLayout: List[CardsLayout] = hands.zip(handsLayoutF).flatMap { case (h, l) => h.map(l) }


      // ----------------------
      // piles
      // ----------------------

      val piles = List(
        props.mySeat.map(_.taken.size),
        props.opponent(0).map(_.taken.size),
        props.opponent(1).map(_.taken.size),
        props.opponent(2).map(_.taken.size)
      )

      val pilesPosition = List(
        layout.table.piles.player0,
        layout.table.piles.player1,
        layout.table.piles.player2,
        layout.table.piles.player3,
      )

      val pilesLayout = piles.zip(pilesPosition).flatMap { case (pile, position) =>
        pile.map(count => CardsLayout.Contracted(position, count, layout.table.piles.sizes))
      }

      // ----------------------
      // deck
      // ----------------------

      val deckLayout: List[CardsLayout] = compactFaceDownCards(props.table.deck.map(_.card), 0)
        .zipWithIndex
        .reverse
        .map { case (cardOrOccurences, col) =>
          cardOrOccurences -> Point(
            layout.table.deck.position.x,
            layout.table.deck.position.y + (col * layout.table.deck.sizes.height * .8)
          )
        }
        .map {
          case (count: Int, position) => CardsLayout.Contracted(position, count, layout.table.deck.sizes)
          case (card: Card, position) => CardsLayout.Expanded(List(card -> position), layout.table.deck.sizes)
        }

      // ----------------------
      // board
      // ----------------------

      val boardLayout: CardsLayout = CardsLayout.Expanded(layout.table.board.positions(props.table.board.flatMap(_.card)), layout.table.board.sizes)


      val renderedCards: List[VdomNode] =
        (boardLayout :: deckLayout ++ pilesLayout ++ handsLayout)
          .flatMap {
            case CardsLayout.Expanded(positions, size) => Some(KGroup(positions.map { case (card, point) => CardComponent(card, size, point) }: _*))
            case CardsLayout.Contracted(position, count, size) if count > 0 => Some(CardComponent(count, size, position))
            case CardsLayout.Contracted(position, count, size) => None
          }

      KGroup(renderedCards: _*)
    }

  def apply(gameProps: GameProps, gameLayout: GameLayout): VdomNode = component((gameProps, gameLayout))
