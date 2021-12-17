package bastoni.domain.logic
package scopa

import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.{Denari, *}
import cats.Applicative

import scala.annotation.tailrec
import scala.util.Random

object Game:

  def playGame[F[_]: Applicative](roomId: RoomId, players: List[User], newId: F[MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    playStream(roomId, GameState(players), playGameStep, s => s.isInstanceOf[GameState.Terminated], messages, newId)

  def playMatch[F[_]: Applicative](roomId: RoomId, players: List[User], newId: F[MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    playStream(roomId, MatchState(players), playMatchStep, s => s == MatchState.Terminated, messages, newId)

  private def playStream[F[_]: Applicative, State](
    targetRoomId: RoomId,
    initialState: State,
    handler: (State, StateMachineInput) => (State, List[StateMachineOutput]),
    isFinal: State => Boolean,
    messages: fs2.Stream[F, Message],
    newId: F[MessageId]
  ): fs2.Stream[F, Message | Delayed[Message]] =
    messages
      .collect { case Message(_, roomId, message) if roomId == targetRoomId => message }
      .scan[(State, List[StateMachineOutput])](initialState -> Nil) {
        case ((state, _), message) => handler(state, message)
      }
      .takeThrough { case ((state, _)) => !isFinal(state) }
      .flatMap { case (_, events) => fs2.Stream.evalSeq(events.toMessages(targetRoomId, newId)) }

  private val uneventful: List[StateMachineOutput] = Nil

  private def withTiemout(state: GameState.PlayRound, request: ActionRequested, before: List[StateMachineOutput] = Nil): (GameState.Active, List[StateMachineOutput]) =
    val newState = GameState.WaitingForPlayer(Timer.ref[GameState](state), Timeout.Max, request, state)
    newState -> (before ++ List(request, newState.nextTick))

  private[scopa] val playGameStep: (GameState, StateMachineInput) => (GameState, List[StateMachineOutput]) =
    (state, event) =>
      playGameStepPF match
        case partialFunction if partialFunction.isDefinedAt(state -> event) => partialFunction(state -> event)
        case _ => state -> uneventful

  private val playGameStepPF: PartialFunction[(GameState, StateMachineInput), (GameState, List[StateMachineOutput])] = {
    case (active: GameState.Active, PlayerLeftTable(player, _)) if active.activePlayers.exists(_.is(player)) =>
      GameState.Aborted -> List(GameAborted)

    case (GameState.Ready(players), GameStarted(_)) =>
      GameState.Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck))

    case (GameState.Ready(matchPlayers), ShuffleDeck(seed)) =>
      val shuffledDeck = new Random(seed).shuffle(Deck.instance)
      val players = matchPlayers.map(Player(_, Nil, Nil))
      val state =
        if (players.size == 4) GameState.Deal5Round(players, shuffledDeck)
        else GameState.Deal3Round(players, Nil, shuffledDeck)
      state -> List(DeckShuffled(shuffledDeck), Continue.later)

    case (GameState.Deal3Round(player :: Nil, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        GameState.WillDealBoardCards(done :+ player.draw(cards), tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (GameState.Deal3Round(player :: todo, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        GameState.Deal3Round(todo, done :+ player.draw(cards), tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (GameState.Deal5Round(player :: done, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        val cardsDealt = CardsDealt(player.id, cards, Direction.Player)
        val players = done :+ player.draw(cards)

        if (tail.isEmpty)
          withTiemout(
            GameState.PlayRound(players, tail, board = Nil),
            ActionRequested(done.head.id, Action.TakeCards, Some(Timeout.Max)),
            List(cardsDealt)
          )
        else GameState.Deal5Round(players, tail) -> List(cardsDealt, Continue.later)
      }

    case (GameState.WillDealBoardCards(players, deck), Continue) =>
      deck.dealOrDie(4) { (boardCards, tail) =>
        withTiemout(
          state = GameState.PlayRound(players, tail, boardCards),
          request = ActionRequested(players.head.id, Action.TakeCards, Some(Timeout.Max)),
          before = List(BoardCardsDealt(boardCards))
        )
      }

    case (GameState.DrawRound(player :: Nil, done, deck, board), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        val players = done :+ player.draw(cards)
        withTiemout(
          state = GameState.PlayRound(players, tail, board),
          request = ActionRequested(players.head.id, Action.TakeCards, Some(Timeout.Max)),
          before = List(CardsDealt(player.id, cards, Direction.Player))
        )
      }

    case (GameState.DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        GameState.DrawRound(todo, done :+ player.draw(cards), tail, trump) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (wait: GameState.WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: GameState.WaitingForPlayer, event) if playGameStepPF.isDefinedAt(wait.state -> event) => playGameStepPF(wait.state -> event)

    case (GameState.PlayRound(player :: nextPlayer :: others, deck, board), TakeCards(p, played, taken)) if player.is(p) && player.has(played) && legalPlay(board, played, taken) =>

      val isLastPlay = nextPlayer.hand.isEmpty && deck.isEmpty

      val (updatedPlayer, updatedBoard, event) =
        if (taken.nonEmpty && board.forall(taken.contains)) {
          // The player has cleared the board
          // This is worth a point if:
          // - it's not the last play
          // - there are 4 players at the table ("scopone scientifico" variant)
          val extraPoint = !isLastPlay || (others.size == 2)
          (
            player.play(played).take(played :: board).addExtraPoints(if (extraPoint) 1 else 0),
            Nil,
            CardsTaken(
              playerId = player.id,
              played = played,
              taken = played :: board,
              extraPoint = extraPoint
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
              played = played,
              taken = played :: board,
              extraPoint = false
            )
          )
        }
        else if (taken.nonEmpty) {
          (
            player.play(played).take(played :: taken),
            board.filterNot(taken.contains),
            CardsTaken(
              playerId = player.id,
              played = played,
              taken = played :: taken,
              extraPoint = false
            )
          )
        }
        else {
          (
            player.play(played),
            played :: board,
            CardsTaken(
              player.id,
              played,
              Nil,
              extraPoint = false
            )
          )
        }

      val updatedPlayers = (nextPlayer :: others) :+ updatedPlayer

      if (nextPlayer.hand.nonEmpty) {
        withTiemout(
          GameState.PlayRound(updatedPlayers, deck, updatedBoard),
          ActionRequested(nextPlayer.id, Action.TakeCards, Some(Timeout.Max)),
          List(event)
        )
      }
      else if (deck.nonEmpty) GameState.DrawRound(updatedPlayers, Nil, deck, updatedBoard) -> List(event, Continue.later)
      else GameState.WillComplete(updatedPlayers) -> List(event, Continue.muchLater)

    case (GameState.WillComplete(players), Continue) =>
      val teams = players match
        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
        case _ => players.map(List(_))

      val scores: List[GameScore] = GameScore(teams)

      val matchScores: List[MatchScore] = teams.zip(scores).map {
        case (players, points) =>
          MatchScore(players.map(_.id), players.head.matchPlayer.win(points.points).points)
      }

      val updatedPlayers: List[MatchPlayer] = players.flatMap { matchPlayer =>
        scores
          .find(_.playerIds.exists(matchPlayer.is))
          .map(pointsCount => matchPlayer.matchPlayer.win(pointsCount.points))
      }

      GameState.Completed(updatedPlayers) -> List(ScopaGameCompleted(scores, matchScores))
  }

  private[scopa] val playMatchStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) = {

    case (MatchState.InProgress(players, gameState, pointsToWin), message) =>
      playGameStep(gameState, message) match
        case (GameState.Completed(players), events) =>
          def ready(shiftedRound: List[MatchPlayer], pointsToWin: Int) =
            MatchState.InProgress(shiftedRound, GameState.Ready(shiftedRound), pointsToWin) -> (events :+ ActionRequested(shiftedRound.last.id, Action.ShuffleDeck))

          val teamSize = if (players.size == 4) 2 else 1

          val winners: List[MatchPlayer] = players.groupBy(_.points).maxBy(_._1)._2
          val winnerPoints = winners.head.points

          if (winnerPoints < pointsToWin || winners.size > teamSize) ready(players.tail :+ players.head, pointsToWin)
          else MatchState.Terminated -> (events :+ MatchCompleted(winners.map(_.id)))

        case (GameState.Aborted, events) =>
          MatchState.Terminated -> (events :+ MatchAborted)

        case (newGameState, events) =>
          MatchState.InProgress(players, newGameState, pointsToWin) -> events

    case (state, _) => state -> uneventful

  }

  def legalPlay(board: List[Card], played: Card, taken: List[Card]): Boolean =
    takeCombinations(board, played).exists(_ == taken.toSet)

  def takeCombinations(board: List[Card], toPlay: Card): Iterator[Set[Card]] =
    if (board.exists(_.rank == toPlay.rank)) board.iterator.filter(_.rank == toPlay.rank).map(Set(_))
    else
      val combinations = for {
        size <- (1 to board.size).iterator
        combination <- board.combinations(size)
        if combination.map(_.rank.value).sum == toPlay.rank.value
      } yield combination.toSet

      if (combinations.isEmpty) Iterator(Set.empty) else combinations
