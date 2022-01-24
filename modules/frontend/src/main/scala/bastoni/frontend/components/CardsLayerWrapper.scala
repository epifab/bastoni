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
  case class TakingCardsState(played: VisibleCard, selected: List[VisibleCard], options: List[Set[VisibleCard]])
  case class State(takingCards: Option[TakingCardsState])

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

      val selectableCards: Map[CardId, Callback] = game.currentTable.mainPlayer.fold(Map.empty)(seat => seat.player match {
        case PlayerState.ActingPlayer(_, Action.PlayCard, _) =>
          seat
            .hand.flatMap(_.card.toOption)
            .map { (card: VisibleCard) => card.ref -> game.callback(FromPlayer.PlayCard(card)) }
            .toMap

        case PlayerState.ActingPlayer(_, Action.PlayCardOf(suit), _) =>
          val hand: List[VisibleCard] = seat.hand.flatMap(_.card.toOption)
          val anyCard: Boolean = hand.forall(_.suit != suit)
          hand.collect {
            case card if card.suit == suit || anyCard =>
              card.ref -> game.callback(FromPlayer.PlayCard(card))
          }.toMap

        case PlayerState.ActingPlayer(_, Action.TakeCards, _) =>
          state.takingCards match {
            case None =>
              seat.hand.flatMap(_.card.toOption).map { cardInHand =>
                val takeCombinations = scopa.Game.takeCombinations(
                  game.currentTable.board.flatMap(_._2.card.toOption),
                  cardInHand
                ).toList

                cardInHand.ref -> {
                  if (takeCombinations == List(Set.empty)) game.callback(TakeCards(cardInHand, Nil))
                  else $.modState(_.copy(takingCards = Some(TakingCardsState(played = cardInHand, selected = Nil, options = takeCombinations))))
                }
              }.toMap

            case Some(TakingCardsState(played, selected, options)) =>
              val board: Map[CardId, Callback] = game.currentTable.board.flatMap {
                case (_, CardPlayerView(card: VisibleCard)) if !selected.contains(card) =>
                  val selectedCombination: Set[VisibleCard] = (card :: selected).toSet
                  val combinations: List[Set[VisibleCard]] = options.filter(c => selectedCombination.forall(c.contains))
                  val completedCombination: Boolean = combinations.contains(selectedCombination)
                  Option.when(combinations.nonEmpty) {
                    card.ref -> {
                      if (completedCombination) game.callback(FromPlayer.TakeCards(played, selectedCombination.toList))
                      else $.modState(_.copy(takingCards = Some(TakingCardsState(played, card :: selected, options))))
                    }
                  }

                case (_, CardPlayerView(card: VisibleCard)) =>
                  Some(card.ref -> $.modState(_.copy(takingCards = Some(TakingCardsState(played, selected.filterNot(_ == card), options)))))

                case _ => None  // practically impossible
              }.toMap

              Map(played.ref -> $.modState(_.copy(takingCards = None))) ++ board
          }

        case _ => Map.empty
      })

      CardsLayer(cards, previousCards, selectableCards, Set.empty)
    }

  private val component =
    ScalaComponent
      .builder[Props]
      .initialState(State(None))
      .renderBackend[Backend]
      .build

  def apply(game: GameState, currentLayout: GameLayout, previousLayout: GameLayout): VdomNode =
    component(Props(game, currentLayout, previousLayout))

