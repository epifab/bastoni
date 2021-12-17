package bastoni.domain.logic
package briscola

import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*

import scala.annotation.tailrec
import scala.util.Random

object Game:

  def playMatch[F[_]](room: Room, messageIds: fs2.Stream[F, MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    playStream(room, MatchState(room.players), playMatchStep, s => s.isInstanceOf[MatchState.Terminated], messages, messageIds)

  def playGame[F[_]](room: Room, messageIds: fs2.Stream[F, MessageId])(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message | Delayed[Message]] =
    playStream(room, GameState(room.players), playGameStep, s => s == GameState.Terminated, messages, messageIds)

  private def playStream[F[_], State](
    room: Room,
    initialState: State,
    handler: (State, Event | Command) => (State, List[Event | Command | Delayed[Command]]),
    isFinal: State => Boolean,
    messages: fs2.Stream[F, Message],
    messageIds: fs2.Stream[F, MessageId]
  ): fs2.Stream[F, Message | Delayed[Message]] =
    messages
      .collect { case Message(_, roomId, message) if roomId == room.id => message }
      .scan[(State, List[Event | Command | Delayed[Command]])](initialState -> Nil) {
        case ((state, _), message) => handler(state, message)
      }
      .takeThrough { case ((state, _)) => !isFinal(state) }
      .flatMap { case (_, events) => events.toMessages(room.id, messageIds) }

  private[briscola] val playMatchStep: (MatchState, Command | Event) => (MatchState, List[Event | Command | Delayed[Command]]) = {

    case (active: MatchState.Active, PlayerLeft(player, _)) if active.activePlayers.exists(_.is(player)) =>
      MatchState.Aborted -> List(MatchAborted)

    case (MatchState.Ready(players), GameStarted(_)) =>
      MatchState.Ready(players) -> List(ActionRequest(players.last.id, Action.ShuffleDeck))

    case (MatchState.Ready(players), ShuffleDeck(seed)) =>
      val shuffledDeck = new Random(seed).shuffle(Deck.instance)

      val deck =
        if (players.size == 3) Some(shuffledDeck.filterNot(_ == Card(Rank.Due, Suit.Coppe)))
        else if (players.size == 2 || players.size == 4) Some(shuffledDeck)
        else None // 1 or 5+ players not supported

      deck.fold(MatchState.Aborted -> List(MatchAborted)) { deck =>
        MatchState.DealRound(
          players.map(MatchPlayer(_, Set.empty, Set.empty)),
          Nil,
          2,
          deck
        ) -> List(DeckShuffled(seed), Continue.later)
      }

    case (MatchState.DealRound(player :: Nil, done, 0, deck), Continue) =>
      deck.dealOrDie { (card, tail) =>
        MatchState.WillDealTrump(done :+ player.draw(card), tail) ->
          List(CardDealt(player.id, card, Face.Player), Continue.later)
      }

    case (MatchState.DealRound(player :: Nil, done, remaining, deck), Continue) =>
      deck.dealOrDie { (card, tail) =>
        MatchState.DealRound(done :+ player.draw(card), Nil, remaining - 1, tail) ->
          List(CardDealt(player.id, card, Face.Player), Continue.shortly)
      }

    case (MatchState.DealRound(player :: todo, done, remaining, deck), Continue) =>
      deck.dealOrDie { (card, tail) =>
        MatchState.DealRound(todo, done :+ player.draw(card), remaining, tail) ->
          List(CardDealt(player.id, card, Face.Player), Continue.shortly)
      }

    case (MatchState.WillDealTrump(players, deck), Continue) =>
      deck.dealOrDie { (card, tail) =>
        MatchState.PlayRound(players, Nil, tail :+ card, card) ->
          List(TrumpRevealed(card), ActionRequest(players.head.id, Action.PlayCard))
      }

    case (MatchState.DrawRound(player :: Nil, done, deck, trump), Continue) =>
      deck.dealOrDie { (card, tail) =>
        val players = done :+ player.draw(card)
        MatchState.PlayRound(players, Nil, tail, trump) ->
          List(CardDealt(player.id, card, Face.Player), ActionRequest(players.head.id, Action.PlayCard))
      }

    case (MatchState.DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.dealOrDie { (card, tail) =>
        MatchState.DrawRound(todo, done :+ player.draw(card), tail, trump) ->
          List(CardDealt(player.id, card, Face.Player), Continue.shortly)
      }

    case (MatchState.PlayRound(player :: Nil, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      MatchState.WillCompleteTrick(done :+ player.play(card), deck, trump) -> List(CardPlayed(player.id, card), Continue.later)

    case (MatchState.PlayRound(player :: next :: players, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      MatchState.PlayRound(next :: players, done :+ player.play(card), deck, trump) ->
        List(CardPlayed(player.id, card), ActionRequest(next.id, Action.PlayCard))

    case (MatchState.WillCompleteTrick(players, deck, trump), Continue) =>
      val updatedPlayers = completeTrick(players, trump)
      val winner = updatedPlayers.head

      val (state, command: (Command | Delayed[Command])) =
        if (deck.isEmpty && winner.hand.isEmpty) MatchState.WillComplete(updatedPlayers, trump) -> Continue.muchLater
        else if (deck.isEmpty) MatchState.PlayRound(updatedPlayers, Nil, Nil, trump) -> ActionRequest(updatedPlayers.head.id, Action.PlayCard)
        else MatchState.DrawRound(updatedPlayers, Nil, deck, trump) -> Continue.later

      state -> (TrickCompleted(winner.id) :: command :: Nil)

    case (MatchState.WillComplete(players, trump), Continue) =>
      val teams = players match
        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
        case ps => ps.map(List(_))

      val pointsCount = teams.map(players => PointsCount(players.map(_.id), players.foldRight(0)(_.points + _)))

      val winners: Option[List[PlayerId]] = pointsCount.sortBy(-_.points) match
        case PointsCount(winners, wp) :: PointsCount(losers, lp) :: _ if wp > lp => Some(winners)
        case _ => None

      val events = pointsCount :+ winners.fold(MatchDraw)(MatchCompleted(_))

      MatchState.Completed(winners.getOrElse(Nil)) -> events

    case (m, _) => m -> Nil
  }

  private[briscola] val playGameStep: (GameState, Command | Event) => (GameState, List[Event | Command | Delayed[Command]]) = {

    case (GameState.InProgress(players, matchState, rounds), message) =>
      playMatchStep(matchState, message) match
        case (MatchState.Completed(winners), events) =>
          val newPlayers = players.map {
            case player if winners.exists(player.is) => player.win
            case player => player
          }

          def ready(shiftedRound: List[GamePlayer], rounds: Int) =
            GameState.InProgress(shiftedRound, MatchState.Ready(shiftedRound), rounds) -> (events :+ ActionRequest(shiftedRound.last.id, Action.ShuffleDeck))

          val teamSize = if (players.size == 4) 2 else 1
          
          if (rounds == 0) {
            val winners = newPlayers.groupMap(_.points)(_.id).maxBy(_._1)._2
            if (winners.size > teamSize) ready(newPlayers.tail :+ newPlayers.head, 0)
            else GameState.Terminated -> (events :+ GameCompleted(winners))
          }
          else ready(newPlayers.tail :+ newPlayers.head, rounds - 1)

        case (MatchState.Aborted, events) =>
          GameState.Terminated -> (events :+ GameAborted)

        case (newMatchState, events) =>
          GameState.InProgress(players, newMatchState, rounds) -> events

    case (state, _) => state -> Nil

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
    val winner: MatchPlayer = trickWinner(None, players).collect(players.map(_(1)).toSet)
    winner :: players.map(_(0)).slideUntil(_.is(winner.player)).tail