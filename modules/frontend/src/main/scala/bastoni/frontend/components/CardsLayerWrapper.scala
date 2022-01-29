package bastoni.frontend.components

import bastoni.domain.logic.scopa
import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.domain.view.FromPlayer.TakeCards
import bastoni.frontend.model.*
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.VdomNode

object CardsLayerWrapper:
  case class TakingCardsState(played: VisibleCard, selected: List[VisibleCard], options: Set[Set[VisibleCard]])

  case class State(takingCards: Option[TakingCardsState], mouseOver: Option[CardId] = None)

  case class Props(game: GameState, currentLayout: GameLayout, previousLayout: GameLayout)

  class Backend($: BackendScope[Props, State]):

    private def pilesLayout(table: TablePlayerView, layout: GameLayout): List[CardLayout] = {
      val data: List[Option[List[CardInstance]]] = List(
        table.opponent1.map(_.taken.map(_.card)),
        table.opponent2.map(_.taken.map(_.card)),
        table.opponent3.map(_.taken.map(_.card)),
        table.mainPlayer.map(_.taken.map(_.card))
      )

      val renderers: List[CardsRenderer] = List(
        layout.player1.renderPile,
        layout.player2.renderPile,
        layout.player3.renderPile,
        layout.mainPlayer.renderPile,
      )

      data.zip(renderers).flatMap { case (d, f) => d.toList.flatMap(f) }
    }

    private def boardLayout(table: TablePlayerView, layout: GameLayout): List[CardLayout] = {
      val players: Map[UserId, TablePlayer] =
        table.mainPlayer.map(_.player.id -> TablePlayer.MainPlayer).toMap ++
          table.opponent1.map(_.player.id -> TablePlayer.Player1).toMap ++
          table.opponent2.map(_.player.id -> TablePlayer.Player2).toMap ++
          table.opponent3.map(_.player.id -> TablePlayer.Player3).toMap

      layout.renderBoard(table.board.map { case (user, card) => user.flatMap(players.get) -> card.card })
    }

    private def handsLayout(table: TablePlayerView, layout: GameLayout): List[CardLayout] = {
      val data: List[Option[List[CardInstance]]] = List(
        table.opponent1.map(_.hand.map(_.card)),
        table.opponent2.map(_.hand.map(_.card)),
        table.opponent3.map(_.hand.map(_.card)),
        table.mainPlayer.map(_.hand.map(_.card))
      )

      val renderers: List[CardsRenderer] = List(
        layout.player1.renderHand,
        layout.player2.renderHand,
        layout.player3.renderHand,
        layout.mainPlayer.renderHand
      )

      data.zip(renderers).flatMap { case (d, f) => d.toList.flatMap(f) }
    }

    private def selectable(game: GameState, state: State): Map[CardId, Callback] = {
      game.currentTable.mainPlayer.fold(Map.empty)(seat => seat.player match {
        case PlayerState.ActingPlayer(_, Action.PlayCard, timeout) if !timeout.contains(Timeout.TimedOut) =>
          seat
            .hand.flatMap(_.card.toOption)
            .map { (card: VisibleCard) => card.ref -> game.sendMessage(FromPlayer.PlayCard(card)) }
            .toMap

        case PlayerState.ActingPlayer(_, Action.PlayCardOf(suit), timeout) if !timeout.contains(Timeout.TimedOut) =>
          val hand: List[VisibleCard] = seat.hand.flatMap(_.card.toOption)
          val anyCard: Boolean = hand.forall(_.suit != suit)
          hand.collect {
            case card if card.suit == suit || anyCard =>
              card.ref -> game.sendMessage(FromPlayer.PlayCard(card))
          }.toMap

        case PlayerState.ActingPlayer(_, Action.TakeCards, timeout) if !timeout.contains(Timeout.TimedOut)=>
          state.takingCards match {
            case None =>
              seat.hand.flatMap(_.card.toOption).map { cardInHand =>
                val takeCombinations: Set[Set[VisibleCard]] = scopa.Game.takeCombinations(
                  game.currentTable.board.flatMap(_._2.card.toOption),
                  cardInHand
                ).toSet

                cardInHand.ref -> {
                  if (takeCombinations.size == 1) game.sendMessage(TakeCards(cardInHand, takeCombinations.head.toList))
                  else $.modState(_.copy(takingCards = Some(TakingCardsState(played = cardInHand, selected = Nil, options = takeCombinations))))
                }
              }.toMap

            case Some(TakingCardsState(played, selected, options)) =>
              Map(played.ref -> $.modState(_.copy(takingCards = None))) ++ game.currentTable.board.flatMap {
                case (_, CardPlayerView(card: VisibleCard)) if !selected.contains(card) =>
                  val selectedComb: Set[VisibleCard] = (card :: selected).toSet
                  val matchingCombs: Set[Set[VisibleCard]] = options.filter(_.intersect(selectedComb) == selectedComb)
                  val completedComb: Boolean = matchingCombs.contains(selectedComb)

                  Option.when(matchingCombs.nonEmpty) {
                    card.ref -> {
                      if (completedComb) $.modState(_.copy(takingCards = None), game.sendMessage(FromPlayer.TakeCards(played, selectedComb.toList)))
                      else $.modState(_.copy(takingCards = Some(TakingCardsState(played, card :: selected, options))))
                    }
                  }

                case (_, CardPlayerView(card: VisibleCard)) =>
                  Some(card.ref -> $.modState(_.copy(takingCards = Some(TakingCardsState(played, selected.filterNot(_ == card), options)))))

                case _ => None  // practically impossible
              }.toMap

          }

        case _ => Map.empty
      })

    }

    def render(props: Props, state: State): VdomNode = {
      val Props(game, currentLayout, previousLayout) = props

      def cardsFor(table: TablePlayerView, layout: GameLayout): List[CardLayout] = {
        layout.deck.renderCards(table.deck.map(_.card)) ++
          pilesLayout(table, layout) ++
          boardLayout(table, layout) ++
          handsLayout(table, layout)
      }

      val cards = cardsFor(game.currentTable, currentLayout)

      val deckOrigin =
        game.currentTable.deck
          .map(cardLayout => cardLayout.card.ref -> previousLayout.deck.controlLayout.copy(card = HiddenCard(cardLayout.card.ref)))
          .toMap

      val previousCards: Map[CardId, CardLayout] =
        game.previousTable.flatMap { previousTable =>
          val allCards: List[(CardId, CardLayout)] =
            cardsFor(previousTable, previousLayout)
              .map(layout => layout.card.ref -> layout)

          Option.when(allCards.nonEmpty)(allCards.toMap)
        }.getOrElse(deckOrigin)

      val cardsEventHandlers: Map[CardId, CardEventHandlers] = selectable(game, state).map {
        case (cardId, callback) => cardId -> CardEventHandlers(
          onSelect = $.modState(_.copy(mouseOver = None), callback),
          onMouseOver = $.modState(_.copy(mouseOver = Some(cardId))),
          onMouseOut = $.modState(_.copy(mouseOver = None))
        )
      }

      CardsLayer(
        cards,
        previousCards,
        cardsEventHandlers,
        selected = state.mouseOver.toSet ++ state.takingCards.fold(Set.empty) { s => s.selected.map(_.ref).toSet + s.played.ref }
      )
    }

  private val component =
    ScalaComponent
      .builder[Props]
      .initialState(State(None))
      .renderBackend[Backend]
      .build

  def apply(game: GameState, currentLayout: GameLayout, previousLayout: GameLayout): VdomNode =
    component(Props(game, currentLayout, previousLayout))

