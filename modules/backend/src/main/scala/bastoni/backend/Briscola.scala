package bastoni.backend

import bastoni.domain.*

import scala.annotation.tailrec
import scala.util.Random

object Briscola extends Game:

  // State machine model
  sealed trait MatchState

  case class  Ready(players: List[GamePlayer]) extends MatchState
  case class  DealRound(todo: List[MatchPlayer], done: List[MatchPlayer], remaining: Int, deck: List[Card]) extends MatchState
  case class  WillDealTrump(players: List[MatchPlayer], deck: List[Card]) extends MatchState
  case class  DrawRound(todo: List[MatchPlayer], done: List[MatchPlayer], deck: List[Card], trump: Card) extends MatchState
  case class  PlayRound(todo: List[MatchPlayer], done: List[(MatchPlayer, Card)], deck: List[Card], trump: Card) extends MatchState
  case class  WillCompleteTrick(players: List[(MatchPlayer, Card)], deck: List[Card], trump: Card) extends MatchState
  case class  WillCompleteMatch(players: List[MatchPlayer], trump: Card) extends MatchState
  case object Terminated extends MatchState

  object EmptyDeckException extends RuntimeException("The deck was empty")

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

  extension(deck: List[Card])
    def deal(f: (Card, List[Card]) => (MatchState, List[Event])): (MatchState, List[Event]) =
      deck match
        case card :: restOfTheDeck => f(card, restOfTheDeck)
        case _ => throw EmptyDeckException

  extension[T](xs: List[T])
    def slideUntil(f: T => Boolean): List[T] =
      (LazyList.from(xs) ++ LazyList.from(xs))
        .dropWhile(!f(_))
        .take(xs.size)
        .toList

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

  def playMatch[F[_]](room: Room)(messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .collect { case Message(roomId, message) if roomId == room.id => message }
      .scan[(MatchState, List[Event])](Ready(room.players.map(p => GamePlayer(p, 0))) -> Nil) {
        case ((state, _), message) => play(state, message)
      }
      .takeThrough { case ((state, _)) => state != Terminated }
      .flatMap { case (_, events) => fs2.Stream.iterable[F, Event](events).map(Message(room.id, _)) }

  private val play: (MatchState, Command | Event) => (MatchState, List[Event]) = {

    case (_, _: PlayerLeft) =>
      Terminated -> List(MatchAborted)

    case (Ready(players), ShuffleDeck(seed)) =>
      val shuffledDeck = new Random(seed).shuffle(Deck.instance)

      val deck =
        if (players.size == 3) Some(shuffledDeck.filterNot(_ == Card(Rank.Due, Suit.Coppe)))
        else if (players.size == 2 || players.size == 4) Some(shuffledDeck)
        else None // 1 or 5+ players not supported

      deck.fold(Terminated -> List(MatchAborted)) { deck =>
        DealRound(
          players.map(MatchPlayer(_, Set.empty, Set.empty)),
          Nil,
          2,
          deck
        ) -> List(DeckShuffled(seed))
      }

    case (DealRound(player :: Nil, done, 0, deck), Continue) =>
      deck.deal { (card, tail) => WillDealTrump(done :+ player.draw(card), tail) -> List(CardDealt(player.id, card)) }

    case (DealRound(player :: Nil, done, remaining, deck), Continue) =>
      deck.deal { (card, tail) => DealRound(done :+ player.draw(card), Nil, remaining - 1, tail) -> List(CardDealt(player.id, card)) }

    case (DealRound(player :: todo, done, remaining, deck), Continue) =>
      deck.deal { (card, tail) => DealRound(todo, done :+ player.draw(card), remaining, tail) -> List(CardDealt(player.id, card)) }

    case (WillDealTrump(players, deck), Continue) =>
      deck.deal { (card, tail) => PlayRound(players, Nil, tail :+ card, card) -> List(TrumpRevealed(card)) }

    case (DrawRound(player :: Nil, done, deck, trump), Continue) =>
      deck.deal { (card, tail) => PlayRound(done :+ player.draw(card), Nil, tail, trump) -> List(CardDealt(player.id, card)) }

    case (DrawRound(player :: todo, done, deck, trump), Continue) =>
      deck.deal { (card, tail) => DrawRound(todo, done :+ player.draw(card), tail, trump) -> List(CardDealt(player.id, card)) }

    case (PlayRound(player :: Nil, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      WillCompleteTrick(done :+ player.play(card), deck, trump) -> List(CardPlayed(player.id, card))

    case (PlayRound(player :: players, done, deck, trump), PlayCard(p, card)) if player.is(p) && player.has(card) =>
      PlayRound(players, done :+ player.play(card), deck, trump) -> List(CardPlayed(player.id, card))

    case (WillCompleteTrick(players, deck, trump), Continue) =>
      val updatedPlayers = completeTrick(players, trump)
      val winner = updatedPlayers.head

      val state =
        if (deck.isEmpty && winner.hand.isEmpty) WillCompleteMatch(updatedPlayers, trump)
        else if (deck.isEmpty) PlayRound(updatedPlayers, Nil, Nil, trump)
        else DrawRound(updatedPlayers, Nil, deck, trump)

      state -> List(TrickWinner(winner.id))

    case (WillCompleteMatch(players, trump), Continue) =>
      val teams = players match
        case a :: b :: c :: d :: Nil => List(List(a, c), List(b, d))
        case ps => ps.map(List(_))

      val pointsCount = teams.map(players => PointsCount(players.map(_.id), players.foldRight(0)(_.points + _)))

      val winners = pointsCount.sortBy(-_.points) match
        case PointsCount(winners, wp) :: PointsCount(losers, lp) :: _ if wp > lp => Some(winners)
        case _ => None

      val events = pointsCount :+ winners.fold(MatchDraw)(MatchWinners(_))

      Terminated -> events

    case (m, _) => m -> Nil
  }
