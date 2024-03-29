package bastoni.domain.logic
package scopa

import bastoni.domain.logic.generic.*
import bastoni.domain.logic.scopa.ScopaGameState.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Delay.syntax.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import cats.Applicative
import io.circe.{Decoder, Encoder}
import io.circe.syntax.EncoderOps

import scala.annotation.tailrec
import scala.util.Random

object ScopaGame extends GenericGameLogic:

  override type GameState = ScopaGameState
  override given stateEncoder: Encoder[ScopaGameState] = ScopaGameState.encoder
  override given stateDecoder: Decoder[ScopaGameState] = ScopaGameState.decoder

  override val gameType: GameType = GameType.Scopa

  override def newMatch(players: List[MatchPlayer]): MatchState.InProgress =
    MatchState.InProgress(players, newGame(players).asJson, MatchType.PointsBased(if (players.size == 2) 11 else 21))
  override def newGame(players: List[MatchPlayer]): ScopaGameState = ScopaGameState.Ready(players)

  override def statusFor(state: ScopaGameState): GameStatus = state match
    case _: ScopaGameState.Active          => GameStatus.InProgress
    case ScopaGameState.Completed(players) => GameStatus.Completed(players)
    case ScopaGameState.Aborted(reason)    => GameStatus.Aborted(reason)

  private def withTimeout(
      state: PlayRound,
      player: UserId,
      action: Action,
      before: List[StateMachineOutput] = Nil
  ): (Active, List[StateMachineOutput]) =
    val request  = Act(player, action, Some(Timeout.Max))
    val newState = WaitingForPlayer(Timer.ref[ScopaGameState](state), Timeout.Max, request, state)
    newState -> (before ++ List(request, newState.nextTick))

  override val playGameStep: (ScopaGameState, StateMachineInput) => (ScopaGameState, List[StateMachineOutput]) =
    (state, event) =>
      playGameStepPF match
        case partialFunction if partialFunction.isDefinedAt(state -> event) => partialFunction(state -> event)
        case _                                                              => state -> uneventful

  val playGameStepPF
      : PartialFunction[(ScopaGameState, StateMachineInput), (ScopaGameState, List[StateMachineOutput])] = {
    case (active: Active, PlayerLeftTable(player, _)) if active.activePlayers.exists(_.is(player)) =>
      Aborted(GameAborted.Reason.playerLeftTheRoom) -> uneventful

    case (Ready(players), MatchStarted(_, _)) =>
      Ready(players) -> List(Act(players.last.id, Action.ShuffleDeck, timeout = None))

    case (Ready(matchPlayers), ShuffleDeck(seed)) =>
      val shuffledDeck = Deck.shuffled(seed)
      val players      = matchPlayers.map(Player(_, Nil, Nil))
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
        val players    = done :+ player.draw(cards)

        if (newDeck.isEmpty)
          withTimeout(
            PlayRound(players, newDeck, board = Nil, lastTake = None),
            done.head.id,
            Action.PlayCard(PlayContext.Scopa),
            List(cardsDealt)
          )
        else Deal5Round(players, newDeck) -> List(cardsDealt, Continue.afterDealingCards)
      }

    case (WillDealBoardCards(players, deck), Continue) =>
      deck.dealOrDie(4) { (boardCards, newDeck) =>
        WillPlay(PlayRound(players, newDeck, boardCards, lastTake = None)) ->
          List(BoardCardsDealt(boardCards), Continue.afterDealingCards)
      }

    case (DrawRound(player :: Nil, done, deck, board, lastTake), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        val players = done :+ player.draw(cards)
        WillPlay(PlayRound(players, newDeck, board, lastTake)) -> List(
          CardsDealt(player.id, cards, Direction.Player),
          Continue.afterDealingCards
        )
      }

    case (DrawRound(player :: todo, done, deck, trump, lastTake), Continue) =>
      deck.dealOrDie(3) { (cards, newDeck) =>
        DrawRound(todo, done :+ player.draw(cards), newDeck, trump, lastTake) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.afterDealingCards)
      }

    case (wait: WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: WaitingForPlayer, event) if playGameStepPF.isDefinedAt(wait.state -> event) =>
      playGameStepPF(wait.state -> event)

    case (WillPlay(playRound), Continue) =>
      withTimeout(
        state = playRound,
        player = playRound.players.head.id,
        action = Action.PlayCard(PlayContext.Scopa)
      )

    case (state @ PlayRound(player :: _, _, board, _), command @ TakeCards(playerId, played, taken))
        if player.is(playerId) && player.has(played) && legalPlay(board, played, taken) =>
      WillTakeCards(state, command) -> List(CardPlayed(playerId, played), Continue.beforeTakingCards)

    case (
          WillTakeCards(PlayRound(player :: nextPlayer :: others, deck, board, lastTake), TakeCards(_, played, taken)),
          Continue
        ) =>
      val isLastPlay      = nextPlayer.hand.isEmpty && deck.isEmpty
      val updatedLastTake = Option.when(taken.nonEmpty)(player.id).orElse(lastTake)

      val (updatedPlayers, updatedBoard, event) =
        if (taken.nonEmpty && board.forall(taken.contains)) {
          // The player has cleared the board
          // This is worth a point if:
          // - it's not the last play
          // - there are 4 players at the table ("scopone scientifico" variant)
          val scopa: Option[VisibleCard] = Option.when(!isLastPlay || (others.size == 2))(played)
          (
            (nextPlayer :: others) :+ player
              .play(played)
              .take(played :: board)
              .addExtraPoints(if (scopa.isDefined) 1 else 0),
            Nil,
            CardsTaken(
              playerId = player.id,
              taken = played :: board,
              scopa = scopa
            )
          )
        } else if (isLastPlay && (lastTake.contains(player.id) || taken.nonEmpty)) {
          // The player takes all remaining cards regardless
          (
            (nextPlayer :: others) :+ player.play(played).take(played :: board),
            Nil,
            CardsTaken(
              playerId = player.id,
              taken = played :: board,
              scopa = None
            )
          )
        } else if (isLastPlay) {
          (
            (nextPlayer :: (others :+ player.play(played))).map {
              case winner if updatedLastTake.contains(winner.id) => winner.take(played :: board)
              case someoneElse                                   => someoneElse
            },
            Nil,
            CardsTaken(
              playerId = updatedLastTake.getOrElse(player.id),
              taken = if (updatedLastTake.isDefined) played :: board else Nil,
              scopa = None
            )
          )
        } else if (taken.nonEmpty) {
          (
            (nextPlayer :: others) :+ player.play(played).take(played :: taken),
            board.filterNot(taken.contains),
            CardsTaken(
              playerId = player.id,
              taken = played :: taken,
              scopa = None
            )
          )
        } else
          (
            (nextPlayer :: others) :+ player.play(played),
            played :: board,
            CardsTaken(
              player.id,
              Nil,
              scopa = None
            )
          )

      if (isLastPlay) WillComplete(updatedPlayers) -> List(event, Continue.beforeGameOver)
      else if (nextPlayer.hand.nonEmpty)
        WillPlay(PlayRound(updatedPlayers, deck, updatedBoard, updatedLastTake)) -> List(
          event,
          Continue.afterTakingCards
        )
      else DrawRound(updatedPlayers, Nil, deck, updatedBoard, updatedLastTake) -> List(event, Continue.afterTakingCards)

    case (WillComplete(players), Continue) =>
      val gameScores: List[ScopaGameScore] = ScopaGameScoreCalculator(Teams(players))

      val updatedPlayers: List[MatchPlayer] = players.flatMap { matchPlayer =>
        gameScores
          .find(_.playerIds.exists(matchPlayer.is))
          .map(score => matchPlayer.matchPlayer.win(score.points))
      }

      val matchScores: List[MatchScore] = MatchScore.forTeams(Teams(updatedPlayers))

      Completed(updatedPlayers) -> List(GameCompleted(gameScores.map(_.generify), matchScores))
  }

  def legalPlay(board: List[VisibleCard], played: VisibleCard, taken: List[VisibleCard]): Boolean =
    takeCombinations(board, played).contains(taken.toSet)

  def takeCombinations(board: List[VisibleCard], toPlay: VisibleCard): Iterator[Set[VisibleCard]] =
    if (board.exists(_.rank == toPlay.rank)) board.iterator.filter(_.rank == toPlay.rank).map(Set(_))
    else
      val combinations = for
        size        <- (1 to board.size).iterator
        combination <- board.combinations(size)
        if combination.map(_.rank.value).sum == toPlay.rank.value
      yield combination.toSet

      if (combinations.isEmpty) Iterator(Set.empty) else combinations
end ScopaGame
