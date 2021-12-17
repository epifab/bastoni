package bastoni.domain.logic
package tressette

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
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
    handler: (State, ServerEvent | Command) => (State, List[ServerEvent | Command | Delayed[Command]]),
    isFinal: State => Boolean,
    messages: fs2.Stream[F, Message],
    newId: F[MessageId]
  ): fs2.Stream[F, Message | Delayed[Message]] =
    messages
      .collect { case Message(_, roomId, message) if roomId == targetRoomId => message }
      .scan[(State, List[ServerEvent | Command | Delayed[Command]])](initialState -> Nil) {
        case ((state, _), message) => handler(state, message)
      }
      .takeThrough { case ((state, _)) => !isFinal(state) }
      .flatMap { case (_, events) => fs2.Stream.evalSeq(events.toMessages(targetRoomId, newId)) }

  private[tressette] val playMatchStep: (MatchState, ServerEvent | Command) => (MatchState, List[ServerEvent | Command | Delayed[Command]]) = {

    case (active: MatchState.Active, PlayerLeftTable(player, _)) if active.activePlayers.exists(_.is(player)) =>
      MatchState.Aborted -> List(MatchAborted)

    case (MatchState.Ready(players), GameStarted(_)) =>
      MatchState.Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck))

    case (MatchState.Ready(players), ShuffleDeck(seed)) =>
      val deck = new Random(seed).shuffle(Deck.instance)
      MatchState.DealRound(
        players.map(MatchPlayer(_, Nil, Nil)),
        Nil,
        1,  // 5 cards at a time: 2 rounds
        deck
      ) -> List(DeckShuffled(deck), Continue.later)

    case (MatchState.DealRound(player :: Nil, done, 0, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        val players = done :+ player.draw(cards)
        MatchState.PlayRound(players, Nil, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), ActionRequested(players.head.id, Action.PlayCard))
      }

    case (MatchState.DealRound(player :: Nil, done, remaining, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        MatchState.DealRound(done :+ player.draw(cards), Nil, remaining - 1, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.shortly)
      }

    case (MatchState.DealRound(player :: todo, done, remaining, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        MatchState.DealRound(todo, done :+ player.draw(cards), remaining, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.shortly)
      }

    case (MatchState.DrawRound(player :: Nil, done, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        val players = done :+ player.draw(card)
        MatchState.PlayRound(players, Nil, tail) ->
          List(CardsDealt(player.id, List(card), Direction.Up), ActionRequested(players.head.id, Action.PlayCard))
      }

    case (MatchState.DrawRound(player :: todo, done, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        MatchState.DrawRound(todo, done :+ player.draw(card), tail) ->
          List(CardsDealt(player.id, List(card), Direction.Up), Continue.shortly)
      }

    case (MatchState.PlayRound(player :: Nil, (firstDone, trump) :: done, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card, trump) =>
      // last player of the trick
      MatchState.WillCompleteTrick((firstDone, trump) :: (done :+ (player.play(card), card)), deck) -> List(CardPlayed(player.id, card), Continue.later)

    case (MatchState.PlayRound(player :: next :: players, Nil, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card) =>
      // first player of the trick (can play any card)
      MatchState.PlayRound(next :: players, (player.play(card), card) :: Nil, deck) -> List(CardPlayed(player.id, card), ActionRequested(next.id, Action.PlayCardOf(card.suit)))

    case (MatchState.PlayRound(player :: next :: players, (firstDone, trump) :: done, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card, trump) =>
      // middle player of the trick
      MatchState.PlayRound(next :: players, (firstDone, trump) :: (done :+ (player.play(card), card)), deck) -> List(CardPlayed(player.id, card), ActionRequested(next.id, Action.PlayCardOf(trump.suit)))

    case (MatchState.WillCompleteTrick(players, deck), Continue) =>
      val updatedPlayers: List[MatchPlayer] = completeTrick(players)
      val winner = updatedPlayers.head

      val (state, command: StateMachineOutput) =
        if (deck.isEmpty && winner.hand.isEmpty) MatchState.WillComplete(updatedPlayers) -> Continue.muchLater
        else if (deck.isEmpty) MatchState.PlayRound(updatedPlayers, Nil, Nil) -> ActionRequested(updatedPlayers.head.id, Action.PlayCard)
        else MatchState.DrawRound(updatedPlayers, Nil, deck) -> Continue.later

      state -> (TrickCompleted(winner.id) :: command :: Nil)

    case (MatchState.WillComplete(players), Continue) =>
      val teams = players match
        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
        case ps => ps.map(List(_))

      // The last trick is called "rete" (net) which will add a point to the final score
      val rete = players.head.id

      val matchPointsCount: List[PointsCount] = teams.map(players =>
        PointsCount(
          playerIds = players.map(_.id),
          points = (players.foldRight(0)(_.points + _) / 3) + (if (players.exists(_.id == rete)) 1 else 0)
        )
      )

      val gamePointsCount: List[PointsCount] = teams.zip(matchPointsCount).map {
        case (players, matchPoints) =>
          PointsCount(players.map(_.id), players.head.gamePlayer.win(matchPoints.points).points)
      }

      // it's not possible to draw so maxBy is good enough
      val winners: List[PlayerId] = matchPointsCount.maxBy(_.points).playerIds

      val updatedPlayers: List[GamePlayer] = players.flatMap { matchPlayer =>
        matchPointsCount
          .find(_.playerIds.exists(matchPlayer.is))
          .map(pointsCount => matchPlayer.gamePlayer.win(pointsCount.points))
      }

      MatchState.Completed(updatedPlayers) -> List(MatchCompleted(winners, matchPointsCount, gamePointsCount))

    case (m, _) => m -> Nil
  }

  private[tressette] val playGameStep: (GameState, ServerEvent | Command) => (GameState, List[ServerEvent | Command | Delayed[Command]]) = {

    case (GameState.InProgress(players, matchState, pointsToWin), message) =>
      playMatchStep(matchState, message) match
        case (MatchState.Completed(players), events) =>

          def ready(shiftedRound: List[GamePlayer], pointsToWin: Int) =
            GameState.InProgress(shiftedRound, MatchState.Ready(shiftedRound), pointsToWin) -> (events :+ ActionRequested(shiftedRound.last.id, Action.ShuffleDeck))

          val teamSize = if (players.size == 4) 2 else 1

          val winners: List[GamePlayer] = players.groupBy(_.points).maxBy(_._1)._2
          val winnerPoints = winners.head.points

          if (winnerPoints < pointsToWin || winners.size > teamSize) ready(players.tail :+ players.head, pointsToWin)
          else GameState.Terminated -> (events :+ GameCompleted(winners.map(_.id)))

        case (MatchState.Aborted, events) =>
          GameState.Terminated -> (events :+ GameAborted)

        case (newMatchState, events) =>
          GameState.InProgress(players, newMatchState, pointsToWin) -> events

    case (state, _) => state -> Nil

  }

  extension(card: Card)
    def points: Int = card.rank match
      case Rank.Asso => 3
      case Rank.Tre => 1
      case Rank.Due => 1
      case Rank.Re => 1
      case Rank.Cavallo => 1
      case Rank.Fante => 1
      case _ => 0

    def value: Int =
      card.rank match
        case Rank.Tre => 10
        case Rank.Due => 9
        case Rank.Asso => 8
        case rank => rank.value - 3

    def >(other: Card): Boolean = value > other.value

  extension(player: MatchPlayer)
    def points: Int = player.taken.foldRight(0)(_.points + _)
    def canPlay(card: Card) = player.has(card)
    def canPlay(card: Card, trump: Card) = player.has(card) && (card.suit == trump.suit || player.hand.forall(_.suit != trump.suit))

  private def completeTrick(players: List[(MatchPlayer, Card)]): List[MatchPlayer] =
    @tailrec
    def trickWinner(winner: Option[(MatchPlayer, Card)], opponents: List[(MatchPlayer, Card)]): MatchPlayer =
      (winner, opponents) match {
        case (Some((winner, card)), Nil) => winner
        case (None, Nil) => throw new IllegalArgumentException("Can't detect the winner for an empty list of players")
        case (None, head :: tail) => trickWinner(Some(head), tail)
        case (Some((winner, winnerCard)), (opponent, opponentCard) :: tail) if winnerCard.suit == opponentCard.suit && opponentCard > winnerCard =>
          trickWinner(Some((opponent, opponentCard)), tail)
        case (winner, _ :: tail) => trickWinner(winner, tail)
      }
    val winner: MatchPlayer = trickWinner(None, players).take(players.map(_(1)))
    winner :: players.map(_(0)).slideUntil(_.is(winner)).tail
