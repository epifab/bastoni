package bastoni.domain.logic
package briscola

import bastoni.domain.logic.briscola.GameState.{PlayRound, WaitingForPlayer}
import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
import cats.Applicative

import scala.annotation.tailrec
import scala.util.Random

object Game extends GameLogic[MatchState]:

  override val gameType: GameType = GameType.Briscola
  override def initialState(users: List[User]): MatchState = MatchState(users)
  override def isFinal(state: MatchState): Boolean = state == MatchState.Terminated

  private def withTimeout(state: PlayRound, player: UserId, action: Action, before: List[StateMachineOutput] = Nil): (GameState.Active, List[StateMachineOutput]) =
    val request = ActionRequested(player, action, Some(Timeout.Max))
    val newState = WaitingForPlayer(Timer.ref[GameState](state), Timeout.Max, request, state)
    newState -> (before ++ List(request, newState.nextTick))

  val playGameStep: (GameState, StateMachineInput) => (GameState, List[StateMachineOutput]) =
    (state, event) =>
      playGameStepPF match
        case partialFunction if partialFunction.isDefinedAt(state -> event) => partialFunction(state -> event)
        case _ => state -> uneventful

  val playGameStepPF: PartialFunction[(GameState, StateMachineInput), (GameState, List[StateMachineOutput])] = {
    case (active: GameState.Active, PlayerLeftTable(user, _)) if active.activePlayers.exists(_.is(user)) =>
      GameState.Aborted -> List(GameAborted)

    case (GameState.Ready(players), GameStarted(_)) =>
      GameState.Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck, timeout = None))

    case (GameState.Ready(players), ShuffleDeck(seed)) =>
      val shuffledDeck = new Random(seed).shuffle(Deck.instance)

      val deck =
        if (players.size == 3) Some(shuffledDeck.filterNot(_ == Card(Rank.Due, Suit.Coppe)))
        else if (players.size == 2 || players.size == 4) Some(shuffledDeck)
        else None // 1 or 5+ players not supported

      deck.fold(GameState.Aborted -> List(GameAborted)) { deck =>
        GameState.DealRound(
          players.map(Player(_, Nil, Nil)),
          Nil,
          deck
        ) -> List(DeckShuffled(deck), Continue.later)
      }

    case (GameState.DealRound(player :: Nil, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        GameState.WillDealTrump(done :+ player.draw(cards), tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (GameState.DealRound(player :: todo, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        GameState.DealRound(todo, done :+ player.draw(cards), tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.shortly)
      }

    case (GameState.WillDealTrump(players, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        withTimeout(
          state = GameState.PlayRound(players, Nil, tail :+ card, card),
          player = players.head.id,
          action = Action.PlayCard,
          before = List(TrumpRevealed(card))
        )
      }

    case (GameState.DrawRound(player :: Nil, done, deck, trump), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        val players = done :+ player.draw(card)
        withTimeout(
          state = GameState.PlayRound(players, Nil, tail, trump),
          player = players.head.id,
          action = Action.PlayCard,
          before = List(CardsDealt(player.id, List(card), Direction.Player))
        )
      }

    case (GameState.DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        GameState.DrawRound(todo, done :+ player.draw(card), tail, trump) ->
          List(CardsDealt(player.id, List(card), Direction.Player), Continue.shortly)
      }

    case (wait: GameState.WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: GameState.WaitingForPlayer, event) if playGameStepPF.isDefinedAt(wait.state -> event) => playGameStepPF(wait.state -> event)

    case (GameState.PlayRound(player :: Nil, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      GameState.WillCompleteTrick(done :+ (player.play(card), card), deck, trump) -> List(CardPlayed(player.id, card), Continue.later)

    case (GameState.PlayRound(player :: next :: players, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      withTimeout(
        state = GameState.PlayRound(next :: players, done :+ (player.play(card), card), deck, trump),
        player = next.id,
        action = Action.PlayCard,
        before = List(CardPlayed(player.id, card))
      )

    case (GameState.WillCompleteTrick(players, deck, trump), Continue) =>
      val updatedPlayers = completeTrick(players, trump)
      val winner = updatedPlayers.head

      val (state, messages: List[StateMachineOutput]) =
        if (deck.isEmpty && winner.hand.isEmpty) GameState.WillComplete(updatedPlayers, trump) -> List(Continue.muchLater)
        else if (deck.nonEmpty) GameState.DrawRound(updatedPlayers, Nil, deck, trump) -> List(Continue.later)
        else withTimeout(state = GameState.PlayRound(updatedPlayers, Nil, Nil, trump), player = updatedPlayers.head.id, action = Action.PlayCard)

      state -> (TrickCompleted(winner.id) :: messages)

    case (GameState.WillComplete(players, trump), Continue) =>
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

      GameState.Completed(updatedPlayers) -> List(BriscolaGameCompleted(scores, matchPoints))
  }

  override val playStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) = {

    case (MatchState.InProgress(matchPlayers, game, rounds), message) =>
      playGameStep(game, message) match
        case (GameState.Completed(players), events) =>

          def ready(updatedPlayers: List[MatchPlayer], rounds: Int) =
            val shiftedRound = updatedPlayers.slideUntil(_.is(matchPlayers.tail.head))
            MatchState.InProgress(shiftedRound, GameState.Ready(shiftedRound), rounds) ->
              (events :+ ActionRequested(shiftedRound.last.id, Action.ShuffleDeck, timeout = None))

          val teamSize = if (players.size == 4) 2 else 1

          if (rounds == 0) {
            players.groupMap(_.points)(_.id).maxBy(_._1)._2 match {
              case winners if winners.size == teamSize => MatchState.Terminated -> (events :+ MatchCompleted(winners))
              case _ => ready(players.tail :+ players.head, 0)
            }
          }
          else ready(players.tail :+ players.head, rounds - 1)

        case (GameState.Aborted, events) =>
          MatchState.Terminated -> (events :+ MatchAborted)

        case (newGameState, events) =>
          MatchState.InProgress(matchPlayers, newGameState, rounds) -> events

    case (state, _) => state -> uneventful

  }

  extension(card: Card)
    def >(other: Card): Boolean =
      val points = GameScore.pointsFor(card)
      val otherPoints = GameScore.pointsFor(other)
      (points > otherPoints) || (points == otherPoints && card.rank.value > other.rank.value)

  private def completeTrick(players: List[(Player, Card)], trump: Card): List[Player] =
    @tailrec
    def trickWinner(winner: Option[(Player, Card)], opponents: List[(Player, Card)]): Player =
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
