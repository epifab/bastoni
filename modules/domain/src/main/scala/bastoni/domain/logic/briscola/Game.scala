package bastoni.domain.logic
package briscola

import bastoni.domain.logic.briscola.GameState.*
import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Delay.syntax.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
import cats.Applicative

import scala.annotation.tailrec
import scala.util.Random

object Game extends GameLogic[MatchState]:

  override val gameType: GameType = GameType.Briscola
  override def initialState(users: List[User]): MatchState = MatchState(users)
  override def isFinal(state: MatchState): Boolean = state == MatchState.Terminated

  private def withTimeout(state: PlayRound, player: UserId, action: Action, before: List[StateMachineOutput] = Nil): (Active, List[StateMachineOutput]) =
    val request = ActionRequested(player, action, Some(Timeout.Max))
    val newState = WaitingForPlayer(Timer.ref[GameState](state), Timeout.Max, request, state)
    newState -> (before ++ List(request, newState.nextTick))

  val playGameStep: (GameState, StateMachineInput) => (GameState, List[StateMachineOutput]) =
    (state, event) =>
      playGameStepPF match
        case partialFunction if partialFunction.isDefinedAt(state -> event) => partialFunction(state -> event)
        case _ => state -> uneventful

  val playGameStepPF: PartialFunction[(GameState, StateMachineInput), (GameState, List[StateMachineOutput])] = {
    case (active: Active, PlayerLeftTable(user, _)) if active.activePlayers.exists(_.is(user)) =>
      Aborted -> List(GameAborted)

    case (Ready(players), GameStarted(_)) =>
      Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck, timeout = None))

    case (Ready(players), ShuffleDeck(seed)) =>
      val shuffledDeck: Deck = Deck.shuffled(seed)

      val deck: Option[Deck] =
        if (players.size == 3) Some(shuffledDeck.discard(Rank.Due, Suit.Coppe))
        else if (players.size == 2 || players.size == 4) Some(shuffledDeck)
        else None // 1 or 5+ players not supported

      deck.fold(Aborted -> List(GameAborted)) { deck =>
        DealRound(
          players.map(Player(_, Nil, Nil)),
          Nil,
          deck
        ) -> List(DeckShuffled(deck), Continue.afterShufflingDeck)
      }

    case (DealRound(player :: Nil, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        WillDealTrump(done :+ player.draw(cards), newDeck) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (DealRound(player :: todo, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        DealRound(todo, done :+ player.draw(cards), newDeck) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (WillDealTrump(players, deck), Continue) =>
      deck.deal1OrDie { (card, newDeck) =>
        WillPlay(PlayRound(players, Nil, newDeck.append(card), card)) -> List(
          TrumpRevealed(card),
          Continue.afterDealingCards
        )
      }

    case (WillPlay(playRound), Continue) =>
      withTimeout(
        state = playRound,
        player = playRound.todo.head.id,
        action = Action.PlayCard
      )

    case (DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.deal1OrDie { (card, newDeck) =>
        DrawRound(todo, done :+ player.draw(card), newDeck, trump) ->
          List(CardsDealt(player.id, List(card), Direction.Player), Continue.afterDealingCards)
      }

    case (DrawRound(Nil, done, deck, trump), Continue) =>
      withTimeout(
        state = PlayRound(done, Nil, deck, trump),
        player = done.head.id,
        action = Action.PlayCard,
        before = Nil
      )

    case (wait: WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: WaitingForPlayer, event) if playGameStepPF.isDefinedAt(wait.state -> event) => playGameStepPF(wait.state -> event)

    case (PlayRound(player :: Nil, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      WillCompleteTrick(done :+ (player.play(card), card), deck, trump) ->
        List(CardPlayed(player.id, card), Continue.beforeTakingCards)

    case (PlayRound(player :: next :: players, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      WillPlay(PlayRound(next :: players, done :+ (player.play(card), card), deck, trump)) -> List(
        CardPlayed(player.id, card),
        Continue.afterPlayingCards
      )

    case (WillCompleteTrick(players, deck, trump), Continue) =>
      val updatedPlayers = completeTrick(players, trump)
      val winner = updatedPlayers.head

      val (state, continue) =
        if (deck.isEmpty && winner.hand.isEmpty) WillComplete(updatedPlayers, trump) -> Continue.beforeGameOver
        else if (deck.nonEmpty) DrawRound(updatedPlayers, Nil, deck, trump) -> Continue.afterPlayingCards
        else WillPlay(PlayRound(updatedPlayers, Nil, deck, trump)) -> Continue.afterPlayingCards

      state -> List(TrickCompleted(winner.id), continue)

    case (WillComplete(players, trump), Continue) =>
      val teams: List[List[Player]] = players match
        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
        case ps => ps.map(List(_))

      val scores: List[GameScore] = teams.map(players => GameScore(players))

      val winners: List[UserId] = scores.winners

      val matchPoints: List[MatchScore] = teams.flatMap(teamPlayers => teamPlayers.headOption.map {
        case winner if winners.exists(winner.is) => MatchScore(teamPlayers.map(_.id), winner.matchPlayer.win.points)
        case loser => MatchScore(teamPlayers.map(_.id), loser.matchPlayer.points)
      })

      val updatedPlayers = players.map {
        case winner if winners.exists(winner.is) => winner.matchPlayer.win
        case loser => loser.matchPlayer
      }

      Completed(updatedPlayers) -> List(BriscolaGameCompleted(scores, matchPoints))
  }

  override val playStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) = {

    case (MatchState.InProgress(matchPlayers, game, rounds), message) =>
      playGameStep(game, message) match
        case (Completed(players), events) =>

          def ready(updatedPlayers: List[MatchPlayer], rounds: Int) =
            val shiftedRound = updatedPlayers.slideUntil(_.is(matchPlayers.tail.head))
            MatchState.InProgress(shiftedRound, Ready(shiftedRound), rounds) ->
              (events :+ ActionRequested(shiftedRound.last.id, Action.ShuffleDeck, timeout = None))

          val teamSize = if (players.size == 4) 2 else 1

          if (rounds == 0) {
            players.groupMap(_.points)(_.id).maxBy(_._1)._2 match {
              case winners if winners.size == teamSize => MatchState.Terminated -> (events :+ MatchCompleted(winners))
              case _ => ready(players.tail :+ players.head, 0)
            }
          }
          else ready(players.tail :+ players.head, rounds - 1)

        case (Aborted, events) =>
          MatchState.Terminated -> (events :+ MatchAborted)

        case (newGameState, events) =>
          MatchState.InProgress(matchPlayers, newGameState, rounds) -> events

    case (state, _) => state -> uneventful

  }

  extension(card: VisibleCard)
    def >(other: VisibleCard): Boolean =
      val points = GameScore.pointsFor(card)
      val otherPoints = GameScore.pointsFor(other)
      (points > otherPoints) || (points == otherPoints && card.rank.value > other.rank.value)

  private def completeTrick(players: List[(Player, VisibleCard)], trump: VisibleCard): List[Player] =
    @tailrec
    def trickWinner(winner: Option[(Player, VisibleCard)], opponents: List[(Player, VisibleCard)]): Player =
      (winner, opponents) match {
        case (Some((winner, card)), Nil) => winner
        case (None, Nil) => throw new IllegalArgumentException("Can't detect the winner for an empty list of players")
        case (None, head :: tail) => trickWinner(Some(head), tail)
        case (Some((winner, winnerCard)), (opponent, opponentCard) :: tail) if winnerCard.suit == opponentCard.suit && opponentCard > winnerCard =>
          trickWinner(Some((opponent, opponentCard)), tail)
        case (Some((winner, winnerCard)), (opponent, opponentCard) :: tail) if winnerCard.suit != opponentCard.suit && opponentCard.suit == trump.suit =>
          trickWinner(Some((opponent -> opponentCard)), tail)
        case (winner, _ :: tail) => trickWinner(winner, tail)
      }
    val winner: Player = trickWinner(None, players).take(players.map(_(1)))
    winner :: players.map(_(0)).slideUntil(_.is(winner)).tail
