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

  def playMatch[F[_]: Applicative](roomId: RoomId, players: List[Player], newId: F[MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    playStream(roomId, MatchState(players), playMatchStep, s => s.isInstanceOf[MatchState.Terminated], messages, newId)

  def playGame[F[_]: Applicative](roomId: RoomId, players: List[Player], newId: F[MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    playStream(roomId, GameState(players), playGameStep, s => s == GameState.Terminated, messages, newId)

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

  private def withTiemout(state: MatchState.PlayRound, request: ActionRequested, before: List[StateMachineOutput] = Nil): (MatchState.Active, List[StateMachineOutput]) =
    val newState = MatchState.WaitingForPlayer(Timer.ref[MatchState](state), Timeout.Max, request, state)
    newState -> (before ++ List(request, newState.nextTick))

  private[scopa] val playMatchStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) =
    (state, event) =>
      playMatchStepPF match
        case partialFunction if partialFunction.isDefinedAt(state -> event) => partialFunction(state -> event)
        case _ => state -> uneventful

  private val playMatchStepPF: PartialFunction[(MatchState, StateMachineInput), (MatchState, List[StateMachineOutput])] = {
    case (active: MatchState.Active, PlayerLeftTable(player, _)) if active.activePlayers.exists(_.is(player)) =>
      MatchState.Aborted -> List(MatchAborted)

    case (MatchState.Ready(players), GameStarted(_)) =>
      MatchState.Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck))

    case (MatchState.Ready(players), ShuffleDeck(seed)) =>
      val shuffledDeck = new Random(seed).shuffle(Deck.instance)

      MatchState.DealRound(
        size = if (players.size == 4) 5 else 3,
        todo = players.map(MatchPlayer(_, Nil, Nil)),
        done = Nil,
        deck = shuffledDeck,
        remaining = if (players.size == 4) 1 else 0
      ) -> List(DeckShuffled(shuffledDeck), Continue.later)

    case (MatchState.DealRound(size, player :: Nil, done, deck, 0), Continue) =>
      deck.dealOrDie(size) { (cards, tail) =>
        MatchState.WillDealBoardCards(done :+ player.draw(cards), tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (MatchState.DealRound(size, player :: Nil, done, deck, remaining), Continue) =>
      deck.dealOrDie(size) { (cards, tail) =>
        MatchState.DealRound(size, Nil, done :+ player.draw(cards), tail, remaining - 1) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (MatchState.DealRound(size, player :: todo, done, deck, remaining), Continue) =>
      deck.dealOrDie(size) { (cards, tail) =>
        MatchState.DealRound(size, todo, done :+ player.draw(cards), tail, remaining) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (MatchState.WillDealBoardCards(players, deck), Continue) =>
      deck.dealOrDie(4) { (boardCards, tail) =>
        withTiemout(
          state = MatchState.PlayRound(players, tail, boardCards),
          request = ActionRequested(players.head.id, Action.TakeCards, Some(Timeout.Max)),
          before = List(BoardCardsDealt(boardCards))
        )
      }

    case (MatchState.DrawRound(player :: Nil, done, deck, board), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        val players = done :+ player.draw(cards)
        withTiemout(
          state = MatchState.PlayRound(players, tail, board),
          request = ActionRequested(players.head.id, Action.TakeCards, Some(Timeout.Max)),
          before = List(CardsDealt(player.id, cards, Direction.Player))
        )
      }

    case (MatchState.DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        MatchState.DrawRound(todo, done :+ player.draw(cards), tail, trump) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (wait: MatchState.WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: MatchState.WaitingForPlayer, event) if playMatchStepPF.isDefinedAt(wait.state -> event) => playMatchStepPF(wait.state -> event)

    case (MatchState.PlayRound(player :: nextPlayer :: others, deck, board), TakeCards(p, played, taken)) if player.is(p) && player.has(played) && legalPlay(board, played, taken) =>

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
          MatchState.PlayRound(updatedPlayers, deck, updatedBoard),
          ActionRequested(nextPlayer.id, Action.TakeCards, Some(Timeout.Max)),
          List(event)
        )
      }
      else if (deck.nonEmpty) MatchState.DrawRound(updatedPlayers, Nil, deck, updatedBoard) -> List(event, Continue.later)
      else MatchState.WillComplete(updatedPlayers) -> List(event, Continue.muchLater)

    case (MatchState.WillComplete(players), Continue) =>
//      val teams: List[List[MatchPlayer]] = players match
//        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
//        case ps => ps.map(List(_))
//
//      val matchPoints = teams.map(players => PointsCount(players.map(_.id), players.foldRight(0)(_.points + _)))
//
//      val winners: List[PlayerId] = matchPoints.sortBy(-_.points) match
//        case PointsCount(winners, wp) :: PointsCount(_, lp) :: _ if wp > lp => winners
//        case _ => Nil
//
//      val gamePoints: List[PointsCount] = teams.flatMap(teamPlayers => teamPlayers.headOption.map {
//        case winner if winners.exists(winner.is) => PointsCount(teamPlayers.map(_.id), winner.gamePlayer.win.points)
//        case loser => PointsCount(teamPlayers.map(_.id), loser.gamePlayer.points)
//      })
//
//      val updatedPlayers = players.map {
//        case winner if winners.exists(winner.is) => winner.gamePlayer.win
//        case loser => loser.gamePlayer
//      }
//
//      MatchState.Completed(updatedPlayers) -> List(MatchCompleted(winners, matchPoints, gamePoints))
  }

  sealed trait Score(val points: Int)

  object Score:
    case class  Primiera(cards: List[Card], value: Int) extends Score(1)
    case object SetteBello extends Score(1)
    case class  NCarte(count: Int) extends Score(1)
    case class  NDenari(count: Int) extends Score(1)
    case class  Scope(count: Int) extends Score(count)

    object Primiera:
      def valueOf(rank: Rank): Int = rank match
        case Sette => 21
        case Sei => 18
        case Asso => 16
        case Cinque => 15
        case Quattro => 14
        case Tre => 13
        case Due => 12
        case Re | Cavallo | Fante => 10

      def apply(cards: List[Card]): Option[Primiera] =
        val groupedBySuit: Map[Suit, List[Card]] = cards.groupBy(_.suit)

        def bestCard(cards: List[Card]): (Card, Int) =
          cards.map(card => card -> valueOf(card.rank)).maxBy(_._2)

        def primiera(groupedBySuit: List[List[Card]]): Primiera =
          val bestCards = groupedBySuit.map(bestCard)
          Primiera(bestCards.map(_._1), bestCards.map(_._2).sum)

        for {
          denari <- groupedBySuit.get(Denari)
          spade <- groupedBySuit.get(Spade)
          coppe <- groupedBySuit.get(Coppe)
          bastoni <- groupedBySuit.get(Bastoni)
        } yield primiera(List(denari, spade, coppe, bastoni))

    def calculate(teams: List[List[MatchPlayer]]): List[ScopaPointsCount] =
      val teamWithCards: Map[List[MatchPlayer], List[Card]] = teams.map(team => team -> team.flatMap(_.taken)).toMap

      val (teamCarte, carte) = teamWithCards.view.mapValues(_.size).mapValues(NCarte(_)).maxBy(_._2.count)
      val (teamDenari, denari) = teamWithCards.view.mapValues(_.filter(_.rank == Denari).size).mapValues(NDenari(_)).maxBy(_._2.count)
      val maybePrimiera = teamWithCards.flatMap { case (team, cards) => Primiera(cards).map(team -> _) }.maxByOption(_._2.value)
      val teamSettebello = teamWithCards.collectFirst { case (team, cards) if cards.contains(Card(Sette, Denari)) => team }

      teams.map(team =>
        new ScopaPointsCount(
          team.map(_.id),
          List(
            Option.when(team == teamCarte)(carte),
            Option.when(team == teamDenari)(denari),
            maybePrimiera.collect { case (teamPrimiera, primiera) if teamPrimiera == team => primiera },
            Option.when(teamSettebello.contains(team))(Score.SetteBello),
            Some(Score.Scope(team.map(_.extraPoints).sum)).filter(_.count > 0)
          ).flatten
        )
      )

  case class ScopaPointsCount(players: List[PlayerId], scores: List[Score]):
    val points: Int = scores.map(_.points).sum

  private[scopa] val playGameStep: (GameState, StateMachineInput) => (GameState, List[StateMachineOutput]) = {

    case (GameState.InProgress(players, matchState, rounds), message) =>
      playMatchStep(matchState, message) match
        case (MatchState.Completed(players), events) =>

          def ready(shiftedRound: List[GamePlayer], rounds: Int) =
            GameState.InProgress(shiftedRound, MatchState.Ready(shiftedRound), rounds) -> (events :+ ActionRequested(shiftedRound.last.id, Action.ShuffleDeck))

          val teamSize = if (players.size == 4) 2 else 1

          if (rounds == 0) {
            players.groupMap(_.points)(_.id).maxBy(_._1)._2 match {
              case winners if winners.size == teamSize => GameState.Terminated -> (events :+ GameCompleted(winners))
              case _ => ready(players.tail :+ players.head, 0)
            }
          }
          else ready(players.tail :+ players.head, rounds - 1)

        case (MatchState.Aborted, events) =>
          GameState.Terminated -> (events :+ GameAborted)

        case (newMatchState, events) =>
          GameState.InProgress(players, newMatchState, rounds) -> events

    case (state, _) => state -> uneventful

  }

  def legalPlay(board: List[Card], played: Card, taken: List[Card]): Boolean =
    val possibleTakes: Iterator[Set[Card]] =
      if (board.exists(_.rank == played.rank)) board.iterator.filter(_.rank == played.rank).map(Set(_))
      else for {
        size <- (1 to board.size).iterator
        combination <- board.combinations(size)
        if combination.map(_.rank.value).sum == played.rank.value
      } yield combination.toSet

    (taken.isEmpty && possibleTakes.isEmpty) || possibleTakes.exists(_ == taken.toSet)
