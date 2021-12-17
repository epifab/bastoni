package bastoni.domain.logic
package briscola

import bastoni.domain.logic.briscola.MatchState.{PlayRound, WaitingForPlayer}
import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
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

  private def withTiemout(state: PlayRound, request: ActionRequested, before: List[StateMachineOutput] = Nil): (MatchState.Active, List[StateMachineOutput]) =
    val newState = WaitingForPlayer(Timer.ref[MatchState](state), Timeout.Max, request, state)
    newState -> (before ++ List(request, newState.nextTick))

  private[briscola] val playMatchStep: (MatchState, StateMachineInput) => (MatchState, List[StateMachineOutput]) =
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

      val deck =
        if (players.size == 3) Some(shuffledDeck.filterNot(_ == Card(Rank.Due, Suit.Coppe)))
        else if (players.size == 2 || players.size == 4) Some(shuffledDeck)
        else None // 1 or 5+ players not supported

      deck.fold(MatchState.Aborted -> List(MatchAborted)) { deck =>
        MatchState.DealRound(
          players.map(MatchPlayer(_, Nil, Nil)),
          Nil,
          deck
        ) -> List(DeckShuffled(deck), Continue.later)
      }

    case (MatchState.DealRound(player :: Nil, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        MatchState.WillDealTrump(done :+ player.draw(cards), tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.later)
      }

    case (MatchState.DealRound(player :: todo, done, deck), Continue) =>
      deck.dealOrDie(3) { (cards, tail) =>
        MatchState.DealRound(todo, done :+ player.draw(cards), tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.shortly)
      }

    case (MatchState.WillDealTrump(players, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        withTiemout(
          state = MatchState.PlayRound(players, Nil, tail :+ card, card),
          request = ActionRequested(players.head.id, Action.PlayCard),
          before = List(TrumpRevealed(card))
        )
      }

    case (MatchState.DrawRound(player :: Nil, done, deck, trump), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        val players = done :+ player.draw(card)
        withTiemout(
          state = MatchState.PlayRound(players, Nil, tail, trump),
          request = ActionRequested(players.head.id, Action.PlayCard),
          before = List(CardsDealt(player.id, List(card), Direction.Player))
        )
      }

    case (MatchState.DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        MatchState.DrawRound(todo, done :+ player.draw(card), tail, trump) ->
          List(CardsDealt(player.id, List(card), Direction.Player), Continue.shortly)
      }

    case (wait: MatchState.WaitingForPlayer, tick: Tick) => wait.ticked(tick)

    case (wait: MatchState.WaitingForPlayer, event) if playMatchStepPF.isDefinedAt(wait.state -> event) => playMatchStepPF(wait.state -> event)

    case (MatchState.PlayRound(player :: Nil, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      MatchState.WillCompleteTrick(done :+ player.play(card), deck, trump) -> List(CardPlayed(player.id, card), Continue.later)

    case (MatchState.PlayRound(player :: next :: players, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      withTiemout(
        state = MatchState.PlayRound(next :: players, done :+ player.play(card), deck, trump),
        request = ActionRequested(next.id, Action.PlayCard),
        before = List(CardPlayed(player.id, card))
      )

    case (MatchState.WillCompleteTrick(players, deck, trump), Continue) =>
      val updatedPlayers = completeTrick(players, trump)
      val winner = updatedPlayers.head

      val (state, messages: List[StateMachineOutput]) =
        if (deck.isEmpty && winner.hand.isEmpty) MatchState.WillComplete(updatedPlayers, trump) -> List(Continue.muchLater)
        else if (deck.nonEmpty) MatchState.DrawRound(updatedPlayers, Nil, deck, trump) -> List(Continue.later)
        else withTiemout(MatchState.PlayRound(updatedPlayers, Nil, Nil, trump), ActionRequested(updatedPlayers.head.id, Action.PlayCard))

      state -> (TrickCompleted(winner.id) :: messages)

    case (MatchState.WillComplete(players, trump), Continue) =>
      val teams: List[List[MatchPlayer]] = players match
        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
        case ps => ps.map(List(_))

      val matchPoints = teams.map(players => PointsCount(players.map(_.id), players.foldRight(0)(_.points + _)))

      val winners: List[PlayerId] = matchPoints.sortBy(-_.points) match
        case PointsCount(winners, wp) :: PointsCount(_, lp) :: _ if wp > lp => winners
        case _ => Nil

      val gamePoints: List[PointsCount] = teams.flatMap(teamPlayers => teamPlayers.headOption.map {
        case winner if winners.exists(winner.is) => PointsCount(teamPlayers.map(_.id), winner.gamePlayer.win.points)
        case loser => PointsCount(teamPlayers.map(_.id), loser.gamePlayer.points)
      })

      val updatedPlayers = players.map {
        case winner if winners.exists(winner.is) => winner.gamePlayer.win
        case loser => loser.gamePlayer
      }

      MatchState.Completed(updatedPlayers) -> List(MatchCompleted(winners, matchPoints, gamePoints))
  }

  private[briscola] val playGameStep: (GameState, StateMachineInput) => (GameState, List[StateMachineOutput]) = {

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

  extension(card: Card)
    def points: Int = card.rank match
      case Rank.Asso => 11
      case Rank.Tre => 10
      case Rank.Re => 4
      case Rank.Cavallo => 3
      case Rank.Fante => 2
      case _ => 0

    def >(other: Card): Boolean =
      (points > other.points) || (points == other.points && card.rank.value > other.rank.value)

  extension(player: MatchPlayer)
    def points: Int = player.collected.foldRight(0)(_.points + _)

  private def completeTrick(players: List[(MatchPlayer, Card)], trump: Card): List[MatchPlayer] =
    @tailrec
    def trickWinner(winner: Option[(MatchPlayer, Card)], opponents: List[(MatchPlayer, Card)]): MatchPlayer =
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
    val winner: MatchPlayer = trickWinner(None, players).collect(players.map(_(1)))
    winner :: players.map(_(0)).slideUntil(_.is(winner.player)).tail
