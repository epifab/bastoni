package bastoni.domain.logic
package briscola

import bastoni.domain.logic.briscola.BriscolaGameState.*
import bastoni.domain.logic.generic.*
import bastoni.domain.model.*
import bastoni.domain.model.Delay.syntax.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
import cats.Applicative
import io.circe.{Encoder, Decoder}
import io.circe.syntax.EncoderOps

import scala.annotation.tailrec
import scala.util.Random

object BriscolaGame extends GenericGameLogic:

  override type GameState = BriscolaGameState
  override given stateEncoder: Encoder[BriscolaGameState] = BriscolaGameState.encoder
  override given stateDecoder: Decoder[BriscolaGameState] = BriscolaGameState.decoder

  override val gameType: GameType = GameType.Briscola

  override def newMatch(players: List[MatchPlayer]): MatchState.InProgress =
    MatchState.InProgress(players, newGame(players).asJson, MatchType.FixedRounds(2))

  override def newGame(players: List[MatchPlayer]): BriscolaGameState =
    BriscolaGameState.Ready(players)

  override def statusFor(state: BriscolaGameState): GameStatus = state match {
    case _: BriscolaGameState.Active => GameStatus.InProgress
    case BriscolaGameState.Completed(players) => GameStatus.Completed(players)
    case BriscolaGameState.Aborted(reason) => GameStatus.Aborted(reason)
  }

  private def withTimeout(state: PlayRound, player: UserId, action: Action, before: List[StateMachineOutput] = Nil): (Active, List[StateMachineOutput]) =
    val request = Act(player, action, Some(Timeout.Max))
    val newState = WaitingForPlayer(Timer.ref[BriscolaGameState](state), Timeout.Max, request, state)
    newState -> (before ++ List(request, newState.nextTick))

  val playGameStep: (BriscolaGameState, StateMachineInput) => (BriscolaGameState, List[StateMachineOutput]) =
    (state, event) =>
      playGameStepPF match
        case partialFunction if partialFunction.isDefinedAt(state -> event) => partialFunction(state -> event)
        case _ => state -> uneventful

  val playGameStepPF: PartialFunction[(BriscolaGameState, StateMachineInput), (BriscolaGameState, List[StateMachineOutput])] = {
    case (active: Active, PlayerLeftRoom(user, _)) if active.activePlayers.exists(_.is(user)) =>
      Aborted(GameAborted.Reason.playerLeftTheRoom) -> uneventful

    case (Ready(players), MatchStarted(_, _)) =>
      Ready(players) -> List(Act(players.last.id, Action.ShuffleDeck, timeout = None))

    case (Ready(players), ShuffleDeck(seed)) =>
      val shuffledDeck: Deck = Deck.shuffled(seed)

      val deck: Option[Deck] =
        if (players.size == 3) Some(shuffledDeck.discard(Rank.Due, Suit.Coppe))
        else if (players.size == 2 || players.size == 4) Some(shuffledDeck)
        else None // 1 or 5+ players not supported

      deck.fold(Aborted(GameAborted.Reason.unexpectedNumberOfPlayers) -> uneventful) { deck =>
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
        action = Action.PlayCard(PlayContext.Briscola(playRound.trump.suit))
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
        action = Action.PlayCard(PlayContext.Briscola(trump.suit)),
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
        else if (deck.nonEmpty) DrawRound(updatedPlayers, Nil, deck, trump) -> Continue.afterTakingCards
        else WillPlay(PlayRound(updatedPlayers, Nil, deck, trump)) -> Continue.afterPlayingCards

      state -> List(TrickCompleted(winner.id), continue)

    case (WillComplete(players, trump), Continue) =>
      val gameScores: List[BriscolaGameScore] = Teams(players).map(players => BriscolaGameScoreCalculator(players))
      val gameWinners: List[UserId] = gameScores.bestTeam

      val updatedPlayers: List[MatchPlayer] = players.map {
        case winner if gameWinners.exists(winner.is) => winner.matchPlayer.win
        case loser => loser.matchPlayer
      }

      val updatedTeams: List[List[MatchPlayer]] = Teams(updatedPlayers)
      val matchScores: List[MatchScore] = MatchScore.forTeams(updatedTeams)

      Completed(updatedPlayers) -> List(GameCompleted(gameScores.map(_.generify), matchScores))
  }

  given order: Ordering[Card] with
    def compare(a: Card, b: Card): Int = {
      val points = BriscolaGameScoreCalculator.pointsFor(a)
      val otherPoints = BriscolaGameScoreCalculator.pointsFor(b)
      (points - otherPoints) match
        case 0 => a.rank.value - b.rank.value
        case x => x
    }

  extension(card: Card)
    def >(other: Card): Boolean = order.compare(card, other) > 0

  def bestCard[C <: Card](trump: Suit, round: List[C]): C =
    @tailrec
    def bestRec(l: List[C], c: Option[C]): C = (l, c) match {
      case (Nil, Some(best)) => best
      case (Nil, None) => throw new IllegalArgumentException("Empty list of cards")
      case (head :: tail, None) => bestRec(tail, Some(head))
      case (head :: tail, Some(best)) if head.suit == best.suit && head > best => bestRec(tail, Some(head))
      case (head :: tail, Some(best)) if head.suit != best.suit && head.suit == trump => bestRec(tail, Some(head))
      case (head :: tail, better) => bestRec(tail, better)
    }
    bestRec(round, None)

  private def completeTrick(players: List[(Player, VisibleCard)], trump: VisibleCard): List[Player] =
    val best: VisibleCard = bestCard(trump.suit, players.map(_._2))
    val winner: Player = players.collectFirst { case (player, card) if best == card => player }.get
    winner.take(players.map(_(1))) :: players.map(_(0)).slideUntil(_.is(winner)).tail
