package bastoni.frontend
package components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.Konva
import org.scalajs.dom.window
import reactkonva.{KCircle, KGroup, KText}


object CardsLayer:

  def renderHands(table: TablePlayerView, layout: GameLayout): List[CardLayout | CardGroupLayout] = {
    val data: List[Option[List[CardInstance]]] = List(
      table.opponent(0).map(_.hand.map(_.card)),
      table.opponent(1).map(_.hand.map(_.card)),
      table.opponent(2).map(_.hand.map(_.card)),
      table.mySeat.map(_.hand.map(_.card))
    )

    val renderers: List[CardsRenderer] = List(
      layout.player1.renderHand,
      layout.player2.renderHand,
      layout.player3.renderHand,
      layout.mainPlayer.renderHand
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

  case class Props(current: List[CardLayout | CardGroupLayout], previous: Map[Int, CardLayout], selectable: Map[Int, Callback])

  object Props:
    def apply(props: GameProps, layout: GameLayout): Props =
      def cardsFor(table: TablePlayerView): List[CardLayout | CardGroupLayout] =
        val players: Map[UserId, TablePlayer] =
          table.mySeat.map(_.player.id -> TablePlayer.MainPlayer).toMap ++
            table.opponent(0).flatMap(_.player).map(_.id -> TablePlayer.Player1).toMap ++
            table.opponent(1).flatMap(_.player).map(_.id -> TablePlayer.Player2).toMap ++
            table.opponent(2).flatMap(_.player).map(_.id -> TablePlayer.Player3).toMap

        layout.renderDeck(table.deck.map(_.card)) ++
          renderPiles(table, layout) ++
          layout.renderBoard(table.board.map { case (user, card) => user.flatMap(players.get) -> card.card }) ++
          renderHands(table, layout)

      val cards = cardsFor(props.currentTable)

      val previousCards: Map[Int, CardLayout] =
        props.previousTable.map { previousTable =>
          cardsFor(previousTable).flatMap {
            case group: CardGroupLayout => group.toCardLayout
            case card: CardLayout => List(card)
          }.map(layout => layout.card.ref -> layout).toMap
        }.getOrElse(Map.empty)

      val selectableCards: Map[Int, Callback] = props.currentTable.mySeat.fold(Map.empty)(seat => seat.player match {
        case PlayerState.ActingPlayer(_, Action.PlayCard, _) =>
          seat
            .hand.flatMap(_.card.toOption)
            .map { (card: VisibleCard) => card.ref -> props.callback(FromPlayer.PlayCard(card)) }
            .toMap

        case PlayerState.ActingPlayer(_, Action.PlayCardOf(suit), _) =>
          val hand: List[VisibleCard] = seat.hand.flatMap(_.card.toOption)
          val anyCard: Boolean = hand.forall(_.suit != suit)
          hand.collect { case card if card.suit == suit || anyCard => card.ref -> props.callback(FromPlayer.PlayCard(card)) }.toMap

        case _ => Map.empty
      })

      new Props(cards, previousCards, selectableCards)

  private val component =
    ScalaComponent
      .builder[Props]
      .stateless
      .render_P { (props: Props) =>
        val renderedCards: List[VdomNode] =
          props.current
            .map {
              case card: CardLayout => CardComponent(card, props.previous.get(card.card.ref), props.selectable.get(card.card.ref))
              case group: CardGroupLayout =>
                KGroup(
                  KGroup(group.toCardLayout.map(card => CardComponent(card, props.previous.get(card.card.ref), props.selectable.get(card.card.ref))): _*),
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
      .shouldComponentUpdate(c => CallbackTo {
        val layoutHasChanged = c.currentProps.current != c.nextProps.current
        val selectableHasChanged = c.currentProps.selectable.keys.toSet != c.nextProps.selectable.keys.toSet
        layoutHasChanged || selectableHasChanged
      })
      .build

  def apply(gameProps: GameProps, gameLayout: GameLayout): VdomNode =
    component(Props(gameProps, gameLayout))
