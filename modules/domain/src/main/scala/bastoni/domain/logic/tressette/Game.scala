package bastoni.domain.logic
package tressette

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
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
      .scan[(State, List[ServerEvent | Command | Delayed[Command]])](initialState -> Nil) {
        case ((state, _), message) => handler(state, message)
      }
      .takeThrough { case ((state, _)) => !isFinal(state) }
      .flatMap { case (_, events) => fs2.Stream.evalSeq(events.toMessages(targetRoomId, newId)) }

  private[tressette] val playGameStep: (GameState, ServerEvent | Command) => (GameState, List[ServerEvent | Command | Delayed[Command]]) = {

    case (active: GameState.Active, PlayerLeftTable(player, _)) if active.activePlayers.exists(_.is(player)) =>
      GameState.Aborted -> List(GameAborted)

    case (GameState.Ready(players), GameStarted(_)) =>
      GameState.Ready(players) -> List(ActionRequested(players.last.id, Action.ShuffleDeck))

    case (GameState.Ready(players), ShuffleDeck(seed)) =>
      val deck = new Random(seed).shuffle(Deck.instance)
      GameState.DealRound(
        players.map(Player(_, Nil, Nil)),
        Nil,
        1,  // 5 cards at a time: 2 rounds
        deck
      ) -> List(DeckShuffled(deck), Continue.later)

    case (GameState.DealRound(player :: Nil, done, 0, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        val players = done :+ player.draw(cards)
        GameState.PlayRound(players, Nil, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), ActionRequested(players.head.id, Action.PlayCard))
      }

    case (GameState.DealRound(player :: Nil, done, remaining, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        GameState.DealRound(done :+ player.draw(cards), Nil, remaining - 1, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.shortly)
      }

    case (GameState.DealRound(player :: todo, done, remaining, deck), Continue) =>
      deck.dealOrDie(5) { (cards, tail) =>
        GameState.DealRound(todo, done :+ player.draw(cards), remaining, tail) ->
          List(CardsDealt(player.id, cards, Direction.Player), Continue.shortly)
      }

    case (GameState.DrawRound(player :: Nil, done, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        val players = done :+ player.draw(card)
        GameState.PlayRound(players, Nil, tail) ->
          List(CardsDealt(player.id, List(card), Direction.Up), ActionRequested(players.head.id, Action.PlayCard))
      }

    case (GameState.DrawRound(player :: todo, done, deck), Continue) =>
      deck.deal1OrDie { (card, tail) =>
        GameState.DrawRound(todo, done :+ player.draw(card), tail) ->
          List(CardsDealt(player.id, List(card), Direction.Up), Continue.shortly)
      }

    case (GameState.PlayRound(player :: Nil, (firstDone, trump) :: done, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card, trump) =>
      // last player of the trick
      GameState.WillCompleteTrick((firstDone, trump) :: (done :+ (player.play(card), card)), deck) -> List(CardPlayed(player.id, card), Continue.later)

    case (GameState.PlayRound(player :: next :: players, Nil, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card) =>
      // first player of the trick (can play any card)
      GameState.PlayRound(next :: players, (player.play(card), card) :: Nil, deck) -> List(CardPlayed(player.id, card), ActionRequested(next.id, Action.PlayCardOf(card.suit)))

    case (GameState.PlayRound(player :: next :: players, (firstDone, trump) :: done, deck), PlayCard(p, card)) if player.is(p) && player.canPlay(card, trump) =>
      // middle player of the trick
      GameState.PlayRound(next :: players, (firstDone, trump) :: (done :+ (player.play(card), card)), deck) -> List(CardPlayed(player.id, card), ActionRequested(next.id, Action.PlayCardOf(trump.suit)))

    case (GameState.WillCompleteTrick(players, deck), Continue) =>
      val updatedPlayers: List[Player] = completeTrick(players)
      val winner = updatedPlayers.head

      val (state, command: StateMachineOutput) =
        if (deck.isEmpty && winner.hand.isEmpty) GameState.WillComplete(updatedPlayers) -> Continue.muchLater
        else if (deck.isEmpty) GameState.PlayRound(updatedPlayers, Nil, Nil) -> ActionRequested(updatedPlayers.head.id, Action.PlayCard)
        else GameState.DrawRound(updatedPlayers, Nil, deck) -> Continue.later

      state -> (TrickCompleted(winner.id) :: command :: Nil)

    case (GameState.WillComplete(players), Continue) =>
      val teams = players match
        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
        case ps => ps.map(List(_))

      // The last trick is called "rete" (net) which will add a point to the final score
      val rete = players.head.id

      val scores: List[GameScore] = teams.map(players => GameScore(players, rete = players.exists(_.id == rete)))

      val matchPointsCount: List[MatchScore] = teams.zip(scores).map {
        case (players, points) =>
          MatchScore(players.map(_.id), players.head.matchPlayer.win(points.points).points)
      }

      val updatedPlayers: List[MatchPlayer] = players.flatMap { matchPlayer =>
        scores
          .find(_.playerIds.exists(matchPlayer.is))
          .map(pointsCount => matchPlayer.matchPlayer.win(pointsCount.points))
      }

      GameState.Completed(updatedPlayers) -> List(TressetteGameCompleted(scores, matchPointsCount))

    case (m, _) => m -> Nil
  }

  private[tressette] val playMatchStep: (MatchState, ServerEvent | Command) => (MatchState, List[ServerEvent | Command | Delayed[Command]]) = {

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

        case (newMatchState, events) =>
          MatchState.InProgress(players, newMatchState, pointsToWin) -> events

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

  extension(player: Player)
    def points: Int = player.taken.foldRight(0)(_.points + _)
    def canPlay(card: Card) = player.has(card)
    def canPlay(card: Card, trump: Card) = player.has(card) && (card.suit == trump.suit || player.hand.forall(_.suit != trump.suit))

  private def completeTrick(players: List[(Player, Card)]): List[Player] =
    @tailrec
    def trickWinner(winner: Option[(Player, Card)], opponents: List[(Player, Card)]): Player =
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
