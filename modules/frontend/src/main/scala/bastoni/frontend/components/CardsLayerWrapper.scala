package bastoni.frontend.components

import bastoni.domain.logic.scopa
import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.domain.view.FromPlayer.TakeCards
import bastoni.frontend.model.*
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.ScalaComponent

object CardsLayerWrapper:
  case class TakingCardsState(played: VisibleCard, selected: List[VisibleCard], options: Set[Set[VisibleCard]])

  case class State(takingCards: Option[TakingCardsState], mouseOver: Option[CardId] = None)

  case class Props(game: GameState, currentLayout: GameLayout, previousLayout: GameLayout)

  class Backend($ : BackendScope[Props, State]):

    private def pilesLayout(room: RoomPlayerView, layout: GameLayout): List[CardLayout] =
      val data: List[Option[List[CardInstance]]] = List(
        room.opponentLeft.map(_.taken.map(_.card)),
        room.opponentFront.map(_.taken.map(_.card)),
        room.opponentRight.map(_.taken.map(_.card)),
        room.mainPlayer.map(_.taken.map(_.card))
      )

      val renderers: List[CardsRenderer] = List(
        layout.player1.renderPile,
        layout.player2.renderPile,
        layout.player3.renderPile,
        layout.mainPlayer.renderPile
      )

      data.zip(renderers).flatMap { case (d, f) => d.toList.flatMap(f) }

    private def boardLayout(room: RoomPlayerView, layout: GameLayout): List[CardLayout] =
      val players: Map[UserId, RoomPlayer] =
        room.mainPlayer.map(_.player.id -> RoomPlayer.MainPlayer).toMap ++
          room.opponentLeft.map(_.player.id -> RoomPlayer.Player1).toMap ++
          room.opponentFront.map(_.player.id -> RoomPlayer.Player2).toMap ++
          room.opponentRight.map(_.player.id -> RoomPlayer.Player3).toMap

      layout.renderBoard(room.board.reverse.map { case BoardCard(card, user) =>
        user.flatMap(players.get) -> card.card
      })

    private def handsLayout(room: RoomPlayerView, layout: GameLayout): List[CardLayout] =
      val data: List[Option[List[CardInstance]]] = List(
        room.opponentLeft.map(_.hand.map(_.card)),
        room.opponentFront.map(_.hand.map(_.card)),
        room.opponentRight.map(_.hand.map(_.card)),
        room.mainPlayer.map(_.hand.map(_.card))
      )

      val renderers: List[CardsRenderer] = List(
        layout.player1.renderHand,
        layout.player2.renderHand,
        layout.player3.renderHand,
        layout.mainPlayer.renderHand
      )

      data.zip(renderers).flatMap { case (d, f) => d.toList.flatMap(f) }

    private def selectable(game: GameState, state: State): Map[CardId, Callback] =
      game.currentRoom.mainPlayer.fold(Map.empty)(seat =>
        seat.player match
          case PlayerState
                .Acting(_, Action.PlayCard(PlayContext.Briscola(_) | PlayContext.Tressette(None)), timeout)
              if !timeout.contains(Timeout.TimedOut) =>
            seat.hand
              .flatMap(_.card.toOption)
              .map { (card: VisibleCard) => card.ref -> game.sendMessage(FromPlayer.PlayCard(card)) }
              .toMap

          case PlayerState.Acting(_, Action.PlayCard(PlayContext.Tressette(Some(suit))), timeout)
              if !timeout.contains(Timeout.TimedOut) =>
            val hand: List[VisibleCard] = seat.hand.flatMap(_.card.toOption)
            val anyCard: Boolean        = hand.forall(_.suit != suit)
            hand.collect {
              case card if card.suit == suit || anyCard =>
                card.ref -> game.sendMessage(FromPlayer.PlayCard(card))
            }.toMap

          case PlayerState.Acting(_, Action.PlayCard(PlayContext.Scopa), timeout)
              if !timeout.contains(Timeout.TimedOut) =>
            state.takingCards match
              case None =>
                seat.hand
                  .flatMap(_.card.toOption)
                  .map { cardInHand =>
                    val takeCombinations: Set[Set[VisibleCard]] = scopa.ScopaGame
                      .takeCombinations(
                        game.currentRoom.board.flatMap(_.card.toOption),
                        cardInHand
                      )
                      .toSet

                    cardInHand.ref -> {
                      if (takeCombinations.size == 1)
                        game.sendMessage(TakeCards(cardInHand, takeCombinations.head.toList))
                      else
                        $.modState(
                          _.copy(takingCards =
                            Some(TakingCardsState(played = cardInHand, selected = Nil, options = takeCombinations))
                          )
                        )
                    }
                  }
                  .toMap

              case Some(TakingCardsState(played, selected, options)) =>
                Map(played.ref -> $.modState(_.copy(takingCards = None))) ++ game.currentRoom.board.flatMap {
                  case BoardCard(CardPlayerView(card: VisibleCard), _) if !selected.contains(card) =>
                    val selectedComb: Set[VisibleCard]       = (card :: selected).toSet
                    val matchingCombs: Set[Set[VisibleCard]] = options.filter(_.intersect(selectedComb) == selectedComb)
                    val completedComb: Boolean               = matchingCombs.contains(selectedComb)

                    Option.when(matchingCombs.nonEmpty) {
                      card.ref -> {
                        if (completedComb)
                          $.modState(
                            _.copy(takingCards = None),
                            game.sendMessage(FromPlayer.TakeCards(played, selectedComb.toList))
                          )
                        else $.modState(_.copy(takingCards = Some(TakingCardsState(played, card :: selected, options))))
                      }
                    }

                  case BoardCard(CardPlayerView(card: VisibleCard), _) =>
                    Some(
                      card.ref -> $.modState(
                        _.copy(takingCards = Some(TakingCardsState(played, selected.filterNot(_ == card), options)))
                      )
                    )

                  case _ => None // practically impossible
                }.toMap

          case _ => Map.empty
      )

    def render(props: Props, state: State): VdomNode =
      val Props(game, currentLayout, previousLayout) = props

      def cardsFor(room: RoomPlayerView, layout: GameLayout): List[CardLayout] =
        layout.deck.renderCards(room.deck.map(_.card)) ++
          pilesLayout(room, layout) ++
          boardLayout(room, layout) ++
          handsLayout(room, layout)

      val cards = cardsFor(game.currentRoom, currentLayout)

      val deckOrigin =
        game.currentRoom.deck
          .map(cardLayout =>
            cardLayout.card.ref -> previousLayout.deck.controlLayout.copy(card = HiddenCard(cardLayout.card.ref))
          )
          .toMap

      val previousCards: Map[CardId, CardLayout] =
        game.previousRoom
          .flatMap { previousRoom =>
            val allCards: List[(CardId, CardLayout)] =
              cardsFor(previousRoom, previousLayout)
                .map(layout => layout.card.ref -> layout)

            Option.when(allCards.nonEmpty)(allCards.toMap)
          }
          .getOrElse(deckOrigin)

      val cardsEventHandlers: Map[CardId, CardEventHandlers] = selectable(game, state).map { case (cardId, callback) =>
        cardId -> CardEventHandlers(
          onSelect = $.modState(_.copy(mouseOver = None), callback),
          onMouseOver = $.modState(_.copy(mouseOver = Some(cardId))),
          onMouseOut = $.modState(_.copy(mouseOver = None))
        )
      }

      CardsLayer(
        cards,
        previousCards,
        cardsEventHandlers,
        selected = state.mouseOver.toSet ++ state.takingCards.fold(Set.empty) { s =>
          s.selected.map(_.ref).toSet + s.played.ref
        }
      )
    end render
  end Backend

  private val component =
    ScalaComponent
      .builder[Props]
      .initialState(State(None))
      .renderBackend[Backend]
      .build

  def apply(game: GameState, currentLayout: GameLayout, previousLayout: GameLayout): VdomNode =
    component(Props(game, currentLayout, previousLayout))
end CardsLayerWrapper
