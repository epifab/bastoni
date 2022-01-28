package bastoni.domain.logic
package tressette

import bastoni.domain.logic.generic.Timer
import bastoni.domain.logic.tressette.GameState.*
import bastoni.domain.logic.tressette.MatchState.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Delay.syntax.*
import bastoni.domain.model.Event.*
import cats.Applicative

import scala.annotation.tailrec
import scala.util.Random

object Game extends GameLogic[MatchState]:

  override val gameType: GameType = GameType.Tressette
  override def initialState(users: List[User]): MatchState & ActiveMatch = MatchState(users)
  override def isFinal(state: MatchState): Boolean = state == Terminated

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

    case (active: Active, PlayerLeftTable(player, _)) if active.activePlayers.exists(_.is(player)) =>
      Aborted -> uneventful

    case (Ready(players), MatchStarted(_, _)) =>
      Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck, timeout = None))

    case (Ready(players), ShuffleDeck(seed)) =>
      if (players.size == 2 || players.size == 4) {
        val deck = Deck.shuffled(seed)
        DealRound(
          players.map(Player(_, Nil, Nil)),
          Nil,
          1, // 5 cards at a time: 2 rounds
          deck
        ) -> List(DeckShuffled(deck), Continue.afterShufflingDeck)
      }
      else Aborted -> uneventful

    case (DealRound(player :: Nil, done, 0, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        val players = done :+ player.draw(cards)
        WillPlay(PlayRound(players, Nil, tail)) -> List(
          CardsDealt(player.id, cards, Direction.Player),
          Continue.afterDealingCards
        )
      }

    case (DealRound(player :: Nil, done, remaining, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        DealRound(done :+ player.draw(cards), Nil, remaining - 1, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (DealRound(player :: todo, done, remaining, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        DealRound(todo, done :+ player.draw(cards), remaining, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (DrawRound(player :: Nil, done, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        val players = done :+ player.draw(card)
        WillPlay(PlayRound(players, Nil, tail)) -> List(
          CardsDealt(player.id, List(card), Direction.Up),
          Continue.afterDealingCards
        )
      }

    case (DrawRound(player :: todo, done, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        DrawRound(todo, done :+ player.draw(card), tail) ->
          List(CardsDealt(player.id, List(card), Direction.Up), Continue.afterDealingCards)
      }

    case (wait: WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: WaitingForPlayer, event) if playGameStepPF.isDefinedAt(wait.state -> event) => playGameStepPF(wait.state -> event)

    case (PlayRound(player :: Nil, (firstDone, trump) :: done, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card, trump) =>
      // last player of the trick
      WillCompleteTrick((firstDone, trump) :: (done :+ (player.play(card), card)), deck) -> List(CardPlayed(player.id, card), Continue.beforeTakingCards)

    case (WillPlay(round), Continue) =>
      withTimeout(
        state = round,
        player = round.todo.head.id,
        action = round.done.headOption.map(_._2.suit).fold(Action.PlayCard)(Action.PlayCardOf.apply)
      )

    case (PlayRound(player :: next :: players, Nil, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card) =>
      // first player of the trick (can play any card)
      WillPlay(PlayRound(next :: players, (player.play(card), card) :: Nil, deck)) ->
        List(CardPlayed(player.id, card), Continue.afterPlayingCards)

    case (PlayRound(player :: next :: players, (firstDone, trump) :: done, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card, trump) =>
      // middle player of the trick
      WillPlay(PlayRound(next :: players, (firstDone, trump) :: (done :+ (player.play(card), card)), deck)) ->
        List(CardPlayed(player.id, card), Continue.afterPlayingCards)

    case (WillCompleteTrick(players, deck), Continue) =>
      val updatedPlayers: List[Player] = completeTrick(players)
      val winner = updatedPlayers.head

      val (state, commands) =
        if (deck.isEmpty && winner.hand.isEmpty) WillComplete(updatedPlayers) -> List(Continue.beforeGameOver)
        else if (deck.nonEmpty) DrawRound(updatedPlayers, Nil, deck) -> List(Continue.afterTakingCards)
        else WillPlay(PlayRound(updatedPlayers, Nil, deck)) -> List(Continue.afterTakingCards)

      state -> (TrickCompleted(winner.id) :: commands)

    case (WillComplete(players), Continue) =>
      // The last trick is called "rete" (net) which will add a point to the final score
      val rete = players.head.id

      val gameScores: List[GameScore] = Teams(players).map(team => GameScoreCalculator(team, rete = team.exists(_.id == rete)))

      val updatedPlayers: List[MatchPlayer] = players.flatMap { matchPlayer =>
        gameScores
          .find(_.playerIds.exists(matchPlayer.is))
          .map(score => matchPlayer.matchPlayer.win(score.points))
      }

      val matchScores: List[MatchScore] = MatchScore.forTeams(Teams(updatedPlayers))

      Completed(updatedPlayers) -> List(GameCompleted(gameScores, matchScores))
  }

  override val playStep: (MatchState, ServerEvent | Command) => (MatchState, List[ServerEvent | Command | Delayed[Command]]) = {

    case (InProgress(matchPlayers, gameState, pointsToWin), message) =>
      playGameStep(gameState, message) match
        case (Completed(players), events) =>

          def newGame(updatedPlayers: List[MatchPlayer], pointsToWin: Int) =
            val shiftedRound = updatedPlayers.slideUntil(_.is(matchPlayers.tail.head))
            GameOver(
              ActionRequested(shiftedRound.last.id, Action.ShuffleDeck, timeout = None),
              InProgress(shiftedRound, Ready(shiftedRound), pointsToWin)
            ) -> (events :+ Continue.afterGameOver)

          val winners = MatchScore.forTeams(Teams(players)).sortBy(-_.points) match {
            case winnerTeam :: secondTeam :: _
              if winnerTeam.points >= pointsToWin && winnerTeam.points > secondTeam.points =>
              Some(winnerTeam)
            case _ => None
          }

          winners.fold(newGame(players.tail :+ players.head, pointsToWin)) { score =>
            GameOver(MatchCompleted(score.playerIds), Terminated) -> (events :+ Continue.afterGameOver)
          }

        case (Aborted, events) =>
          GameOver(MatchAborted, Terminated) -> (events :+ Event.GameAborted :+ Continue.afterGameOver)

        case (newMatchState, events) =>
          InProgress(matchPlayers, newMatchState, pointsToWin) -> events

    case (GameOver(event, newState), _) => newState -> List(event)

    case (state, _) => state -> Nil

  }

  extension(card: Card)
    def value: Int =
      card.rank match
        case Rank.Tre => 10
        case Rank.Due => 9
        case Rank.Asso => 8
        case rank => rank.value - 3

    def >(other: Card): Boolean = value > other.value

  extension(player: Player)
    def canPlay(card: VisibleCard) = player.has(card)
    def canPlay(card: VisibleCard, trump: VisibleCard) = player.has(card) && (card.suit == trump.suit || player.hand.forall(_.suit != trump.suit))

  private def completeTrick(players: List[(Player, VisibleCard)]): List[Player] =
    @tailrec
    def trickWinner(winner: Option[(Player, VisibleCard)], opponents: List[(Player, VisibleCard)]): Player =
      (winner, opponents) match {
        case (Some((winner, card)), Nil) => winner
        case (None, Nil) => throw new IllegalArgumentException("Can't detect the winner for an empty list of players")
        case (None, head :: tail) => trickWinner(Some(head), tail)
        case (Some((winner, winnerCard)), (opponent, opponentCard) :: tail) if winnerCard.suit == opponentCard.suit && opponentCard > winnerCard =>
          trickWinner(Some((opponent, opponentCard)), tail)
        case (winner, _ :: tail) => trickWinner(winner, tail)
      }
    val winner: Player = trickWinner(None, players).take(players.map(_(1)))
    winner :: players.map(_(0)).slideUntil(_.is(winner)).tail
