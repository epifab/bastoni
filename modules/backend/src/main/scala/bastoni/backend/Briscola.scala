package bastoni.backend

import bastoni.domain.*

import scala.annotation.tailrec
import scala.util.Random

object Briscola:

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

  extension(players: List[MatchPlayer])
    def winners: List[MatchPlayer] =
      players.foldLeft[List[MatchPlayer]](Nil) {
        case (Nil, player) => List(player)
        case (winner :: _, player) if player.points > winner.points => List(player)
        case (winner :: winners, player) if player.points == winner.points => winner :: (winners :+ player)
        case (winners, _) => winners
      }

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

  def apply[F[_]](room: Room, messages: fs2.Stream[F, Message]): fs2.Stream[F, Message] =
    messages
      .collect[Event | Command] { case Message(roomId, message) if roomId == room.id => message }
      .scan[(MatchState, List[Event])](Ready(room.players.map(p => GamePlayer(p, 0))) -> Nil) {

        case (_, _: PlayerLeft) =>
          Terminated -> List(GameAborted)

        case ((Ready(players), _), ShuffleDeck(seed)) =>
          val shuffledDeck = new Random(seed).shuffle(Deck.instance)
          val strippedDeck = if (room.players.size == 3) shuffledDeck.filterNot(_ == Card(Rank.Due, Suit.Coppe)) else shuffledDeck
          DealRound(players.map(MatchPlayer(_, Set.empty, Set.empty)), Nil, 2, strippedDeck) ->
            List(DeckShuffled(seed))

        case ((DealRound(player :: Nil, done, 0, deck), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => WillDealTrump(done :+ player.draw(card), tail) -> List(CardDealt(player.id, card)) }

        case ((DealRound(player :: Nil, done, remaining, deck), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => DealRound(done :+ player.draw(card), Nil, remaining - 1, tail) -> List(CardDealt(player.id, card)) }

        case ((DealRound(player :: todo, done, remaining, deck), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => DealRound(todo, done :+ player.draw(card), remaining, tail) -> List(CardDealt(player.id, card)) }

        case ((WillDealTrump(players, deck), _), Continue) =>
          deck.deal { (card, tail) => PlayRound(players, Nil, tail :+ card, card) -> List(TrumpRevealed(card)) }

        case ((DrawRound(player :: Nil, done, deck, trump), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => PlayRound(done :+ player.draw(card), Nil, tail, trump) -> List(CardDealt(player.id, card)) }

        case ((DrawRound(player :: todo, done, deck, trump), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => DrawRound(todo, done :+ player.draw(card), tail, trump) -> List(CardDealt(player.id, card)) }

        case ((PlayRound(player :: Nil, done, deck, trump), _), PlayCard(p, card)) if player.is(p) && player.has(card) =>
          WillCompleteTrick(done :+ player.play(card), deck, trump) -> List(CardPlayed(player.id, card))

        case ((PlayRound(player :: players, done, deck, trump), _), PlayCard(p, card)) if player.is(p) && player.has(card) =>
          PlayRound(players, done :+ player.play(card), deck, trump) -> List(CardPlayed(player.id, card))

        case ((WillCompleteTrick(players, deck, trump), _), Continue) =>
          val updatedPlayers = completeTrick(players, trump)
          val winner = updatedPlayers.head

          val state =
            if (deck.isEmpty && winner.hand.isEmpty) WillCompleteMatch(updatedPlayers, trump)
            else if (deck.isEmpty) PlayRound(updatedPlayers, Nil, Nil, trump)
            else DrawRound(updatedPlayers, Nil, deck, trump)

          state -> List(TrickWinner(winner.id))

        case ((WillCompleteMatch(players, trump), _), Continue) =>
          val matchWinner = players.winners match
            case winner :: Nil => Some(winner)
            case _ => None

          val events = players.map(p => PointsCount(p.id, p.points)) :+ matchWinner.fold(MatchDraw)(p => MatchWinner(p.id))

          val state = Ready(players.map {
            case player if matchWinner.contains(player) => player.gamePlayer.win
            case player => player.gamePlayer
          })

          state -> events

        case ((m, _), _) => m -> Nil
      }
      .takeThrough { case ((state, _)) => state != Terminated }
      .flatMap { case (_, events) => fs2.Stream.iterable[F, Event](events).map(Message(room.id, _)) }
