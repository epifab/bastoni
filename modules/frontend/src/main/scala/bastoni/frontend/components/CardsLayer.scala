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
        .reverse
        .map {
          case count: Int =>
            CardsLayout.Contracted(
              Point(
                layout.table.deck.position.x,
                layout.table.deck.position.y
              ),
              count,
              layout.table.deck.sizes
            )
          case card: Card =>
            CardsLayout.Expanded(
              List(card -> Point(
                layout.table.deck.position.x + (layout.table.deck.sizes.width * .5),
                layout.table.deck.position.y + (layout.table.deck.sizes.height * .8)
              )),
              layout.table.deck.sizes,
              rotation = Some(23)
            )
        }

      // ----------------------
      // board
      // ----------------------

      val boardLayout: CardsLayout = CardsLayout.Expanded(layout.table.board.positions(props.table.board.flatMap(_.card)), layout.table.board.sizes)


      val renderedCards: List[VdomNode] =
        (boardLayout :: deckLayout ++ pilesLayout ++ handsLayout)
          .flatMap {
            case CardsLayout.Expanded(positions, size, rotation) => Some(KGroup(positions.map { case (card, position) => CardComponent(card, size, position, rotation) }: _*))
            case CardsLayout.Contracted(position, count, size, rotation) if count > 0 => Some(CardComponent(count, size, position, rotation))
            case CardsLayout.Contracted(position, count, size, rotation) => None
          }

      KGroup(renderedCards: _*)
    }

  def apply(gameProps: GameProps, gameLayout: GameLayout): VdomNode = component((gameProps, gameLayout))
