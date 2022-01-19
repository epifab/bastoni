package bastoni.domain.logic
package scopa

import bastoni.domain.logic.generic.Timer
import bastoni.domain.logic.scopa.GameState.*
import bastoni.domain.logic.scopa.MatchState.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Delay.syntax.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import cats.Applicative

import scala.annotation.tailrec
import scala.util.Random

object Game extends GameLogic[MatchState]:

  override val gameType: GameType = GameType.Scopa
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
      Aborted -> List(GameAborted)

    case (Ready(players), MatchStarted(_, _)) =>
      Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck, timeout = None))

    case (Ready(matchPlayers), ShuffleDeck(seed)) =>
      val shuffledDeck = Deck.shuffled(seed)
      val players = matchPlayers.map(Player(_, Nil, Nil))
      val state =
        if (players.size == 4) Deal5Round(players, shuffledDeck)
        else Deal3Round(players, Nil, shuffledDeck)
      state -> List(DeckShuffled(shuffledDeck), Continue.afterShufflingDeck)

    case (Deal3Round(player :: Nil, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        WillDealBoardCards(done :+ player.draw(cards), newDeck) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (Deal3Round(player :: todo, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        Deal3Round(todo, done :+ player.draw(cards), newDeck) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (Deal5Round(player :: done, deck), Continue) =>
      deck.dealOrDie(5) { (cards, newDeck) =>
        val cardsDealt = CardsDealt(player.id, cards, Direction.Player)
        val players = done :+ player.draw(cards)

        if (newDeck.isEmpty)
          withTimeout(
            PlayRound(players, newDeck, board = Nil),
            done.head.id,
            Action.TakeCards,
            List(cardsDealt)
          )
        else Deal5Round(players, newDeck) -> List(cardsDealt, Continue.afterDealingCards)
      }

    case (WillDealBoardCards(players, deck), Continue) =>
      deck.dealOrDie(4) { (boardCards, newDeck) =>
        WillPlay(PlayRound(players, newDeck, boardCards)) ->
          List(BoardCardsDealt(boardCards), Continue.afterDealingCards)
      }

    case (DrawRound(player :: Nil, done, deck, board), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        val players = done :+ player.draw(cards)
        WillPlay(PlayRound(players, newDeck, board)) -> List(
          CardsDealt(player.id, cards, Direction.Player),
          Continue.afterDealingCards
        )
      }

    case (DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        DrawRound(todo, done :+ player.draw(cards), newDeck, trump) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (wait: WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: WaitingForPlayer, event) if playGameStepPF.isDefinedAt(wait.state -> event) => playGameStepPF(wait.state -> event)

    case (WillPlay(playRound), Continue) =>
      withTimeout(
        state = playRound,
        player = playRound.players.head.id,
        action = Action.TakeCards
      )

    case (state@ PlayRound(player :: _, _, board), command@ TakeCards(playerId, played, taken)) if player.is(playerId) && player.has(played) && legalPlay(board, played, taken) =>
      WillTakeCards(state, command) -> List(CardPlayed(playerId, played), Continue.beforeTakingCards)

    case (WillTakeCards(PlayRound(player :: nextPlayer :: others, deck, board), TakeCards(_, played, taken)), Continue) =>

      val isLastPlay = nextPlayer.hand.isEmpty && deck.isEmpty

      val (updatedPlayer, updatedBoard, event) =
        if (taken.nonEmpty && board.forall(taken.contains)) {
          // The player has cleared the board
          // This is worth a point if:
          // - it's not the last play
          // - there are 4 players at the table ("scopone scientifico" variant)
          val scopa: Option[VisibleCard] = Option.when(!isLastPlay || (others.size == 2))(played)
          (
            player.play(played).take(played :: board).addExtraPoints(if (scopa.isDefined) 1 else 0),
            Nil,
            CardsTaken(
              playerId = player.id,
              taken = played :: board,
              scopa = scopa
            )
          )
        }
        else if (isLastPlay) {
          // The player takes all remaining cards regardless
          (
            player.play(played).take(played :: board),
            Nil,
            CardsTaken(
              playerId = player.id,
              taken = played :: board,
              scopa = None
            )
          )
        }
        else if (taken.nonEmpty) {
          (
            player.play(played).take(played :: taken),
            board.filterNot(taken.contains),
            CardsTaken(
              playerId = player.id,
              taken = played :: taken,
              scopa = None
            )
          )
        }
        else {
          (
            player.play(played),
            played :: board,
            CardsTaken(
              player.id,
              Nil,
              scopa = None
            )
          )
        }

      val updatedPlayers = (nextPlayer :: others) :+ updatedPlayer

      if (nextPlayer.hand.nonEmpty) WillPlay(PlayRound(updatedPlayers, deck, updatedBoard)) -> List(event, Continue.afterTakingCards)
      else if (deck.nonEmpty) DrawRound(updatedPlayers, Nil, deck, updatedBoard) -> List(event, Continue.afterTakingCards)
      else WillComplete(updatedPlayers) -> List(event, Continue.beforeGameOver)

    case (WillComplete(players), Continue) =>
      val gameScores: List[GameScore] = GameScoreCalculator(Teams(players))

      val updatedPlayers: List[MatchPlayer] = players.flatMap { matchPlayer =>
        gameScores
          .find(_.playerIds.exists(matchPlayer.is))
          .map(score => matchPlayer.matchPlayer.win(score.points))
      }

      val matchScores: List[MatchScore] = MatchScore.forTeams(Teams(updatedPlayers))

      Completed(updatedPlayers) -> List(GameCompleted(gameScores, matchScores))
  }

  override val playStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) = {

    case (InProgress(matchPlayers, gameState, pointsToWin), message) =>
      playGameStep(gameState, message) match
        case (Completed(players), events) =>
          def newGame(updatedPlayers: List[MatchPlayer], pointsToWin: Int) =
            val shiftedRound = updatedPlayers.slideUntil(_.is(matchPlayers.tail.head))
            GameOver(
              ActionRequested(shiftedRound.last.id, Action.ShuffleDeck, timeout = None),
              InProgress(shiftedRound, Ready(shiftedRound), pointsToWin)
            ) -> (events :+ Continue.afterGameOver)

          val teamSize = Teams.size(players)

          val winners: List[MatchPlayer] = players.groupBy(_.points).maxBy(_._1)._2
          val winnerPoints = winners.head.points

          if (winnerPoints < pointsToWin || winners.size > teamSize) newGame(players.tail :+ players.head, pointsToWin)
          else GameOver(MatchCompleted(winners.map(_.id)), Terminated) -> (events :+ Continue.afterGameOver)

        case (Aborted, events) =>
          GameOver(MatchAborted, Terminated) -> (events :+ Continue.afterGameOver)

        case (newGameState, events) =>
          InProgress(matchPlayers, newGameState, pointsToWin) -> events

    case (GameOver(event, state), Continue) => state -> List(event)

    case (state, _) => state -> uneventful

  }

  def legalPlay(board: List[VisibleCard], played: VisibleCard, taken: List[VisibleCard]): Boolean =
    takeCombinations(board, played).contains(taken.toSet)

  def takeCombinations(board: List[VisibleCard], toPlay: VisibleCard): Iterator[Set[VisibleCard]] =
    if (board.exists(_.rank == toPlay.rank)) board.iterator.filter(_.rank == toPlay.rank).map(Set(_))
    else
      val combinations = for {
        size <- (1 to board.size).iterator
        combination <- board.combinations(size)
        if combination.map(_.rank.value).sum == toPlay.rank.value
      } yield combination.toSet

      if (combinations.isEmpty) Iterator(Set.empty) else combinations
