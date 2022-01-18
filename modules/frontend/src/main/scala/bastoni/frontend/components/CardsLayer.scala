package bastoni.frontend
package components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.Konva
import org.scalajs.dom.window
import reactkonva.{KCircle, KGroup, KLayer, KText}


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
    def apply(props: GameState, currentLayout: GameLayout, previousLayout: GameLayout): Props =
      def cardsFor(table: TablePlayerView, layout: GameLayout): List[CardLayout | CardGroupLayout] =
        val players: Map[UserId, TablePlayer] =
          table.mySeat.map(_.player.id -> TablePlayer.MainPlayer).toMap ++
            table.opponent(0).flatMap(_.player).map(_.id -> TablePlayer.Player1).toMap ++
            table.opponent(1).flatMap(_.player).map(_.id -> TablePlayer.Player2).toMap ++
            table.opponent(2).flatMap(_.player).map(_.id -> TablePlayer.Player3).toMap

        layout.deck.renderCards(table.deck.map(_.card)) ++
          renderPiles(table, layout) ++
          layout.renderBoard(table.board.map { case (user, card) => user.flatMap(players.get) -> card.card }) ++
          renderHands(table, layout)

      val cards = cardsFor(props.currentTable, currentLayout)

      val previousCards: Map[Int, CardLayout] =
        props.previousTable.map { previousTable =>
          cardsFor(previousTable, previousLayout).flatMap {
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
        val renderedCards: List[VdomElement] =
          props.current
            .flatMap {
              case card: CardLayout => List(card)
              case group: CardGroupLayout => group.toCardLayout
            }
            .map(layout => CardComponent(layout, props.previous.get(layout.card.ref), props.selectable.get(layout.card.ref)))

        KLayer(renderedCards: _*)
      }
      .shouldComponentUpdate(c => CallbackTo {
        val layoutHasChanged = c.currentProps.current != c.nextProps.current
        val selectableHasChanged = c.currentProps.selectable.keys.toSet != c.nextProps.selectable.keys.toSet
        layoutHasChanged || selectableHasChanged
      })
      .build

  def apply(gameProps: GameState, currentLayout: GameLayout, previousLayout: GameLayout): VdomNode =
    component(Props(gameProps, currentLayout, previousLayout))
