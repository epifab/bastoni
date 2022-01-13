package bastoni.frontend
package components

import bastoni.domain.model.*
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.Konva
import org.scalajs.dom.window
import reactkonva.{KCircle, KGroup, KText}


object CardsLayer:

  def renderHands(table: TablePlayerView, layout: GameLayout): List[CardLayout | CardGroupLayout] = {
    val data: List[Option[List[CardInstance]]] = List(
      table.mySeat.map(_.hand.map(_.card)),
      table.opponent(0).map(_.hand.map(_.card)),
      table.opponent(1).map(_.hand.map(_.card)),
      table.opponent(2).map(_.hand.map(_.card)),
    )

    val renderers: List[CardsRenderer] = List(
      layout.mainPlayer.renderHand,
      layout.player1.renderHand,
      layout.player2.renderHand,
      layout.player3.renderHand
    )

    data.zip(renderers).flatMap { case (d, f) => d.toList.flatMap(f) }
  }

  def renderPiles(table: TablePlayerView, layout: GameLayout): List[CardLayout | CardGroupLayout] = {
    val data: List[Option[List[CardInstance]]] = List(
      table.mySeat.map(_.taken.map(_.card)),
      table.opponent(0).map(_.taken.map(_.card)),
      table.opponent(1).map(_.taken.map(_.card)),
      table.opponent(2).map(_.taken.map(_.card))
    )

    val renderers: List[CardsRenderer] = List(
      layout.mainPlayer.renderPile,
      layout.player1.renderPile,
      layout.player2.renderPile,
      layout.player3.renderPile,
    )

    data.zip(renderers).flatMap { case (d, f) => d.toList.flatMap(f) }
  }

  case class Props(current: List[CardLayout | CardGroupLayout], previous: Map[Int, CardLayout])

  object Props:
    def apply(props: GameProps, layout: GameLayout): Props =
      def cardsFor(table: TablePlayerView): List[CardLayout | CardGroupLayout] =
        layout.renderDeck(table.deck.map(_.card)) ++
          renderPiles(table, layout) ++
          layout.renderBoard(table.board.map(_.card)) ++
          renderHands(table, layout)

      val cards = cardsFor(props.currentTable)

      val previousCards: Map[Int, CardLayout] =
        props.previousTable.map { previousTable =>
          cardsFor(previousTable).flatMap {
            case group: CardGroupLayout => group.toCardLayout
            case card: CardLayout => List(card)
          }.map(layout => layout.card.ref -> layout).toMap
        }.getOrElse(Map.empty)

      new Props(cards, previousCards)

  private val component =
    ScalaComponent
      .builder[Props]
      .stateless
      .render_P { props =>
        val renderedCards: List[VdomNode] =
          props.current
            .map {
              case card: CardLayout => CardComponent(card, props.previous.get(card.card.ref))
              case group: CardGroupLayout =>
                KGroup(
                  KGroup(group.toCardLayout.map(card => CardComponent(card, props.previous.get(card.card.ref))): _*),
//                  KGroup(
//                    { p =>
//                      p.x = group.topLeft.x
//                      p.y = group.topLeft.y
//                    },
//                    KCircle(
//                      { p =>
//                        p.radius = (group.cardSize.width - 5) / 2
//                        p.x = group.cardSize.width / 2
//                        p.y = group.cardSize.height / 2
//                        p.fill = "#222"
//                        p.stroke = "#FFF"
//                        p.strokeWidth = 3
//                      }
//                    ),
//                    KText(
//                      { p =>
//                        p.text = group.cards.length.toString
//                        p.height = group.cardSize.height
//                        p.width = group.cardSize.width
//                        p.fontFamily = "'Open Sans', sans-serif"
//                        p.fontStyle = "bold"
//                        p.fill = "#FFF"
//                        p.align = "center"
//                        p.verticalAlign = "middle"
//                      }
//                    )
//                  )
                )
            }

        KGroup(renderedCards: _*)
      }
      .shouldComponentUpdate(c => CallbackTo(c.currentProps.current != c.nextProps.current))
      .build

  def apply(gameProps: GameProps, gameLayout: GameLayout): VdomNode =
    component(Props(gameProps, gameLayout))
