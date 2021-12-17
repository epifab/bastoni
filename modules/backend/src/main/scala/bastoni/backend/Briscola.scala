package bastoni.backend

import bastoni.domain.*

import scala.annotation.tailrec
import scala.util.Random

object Briscola:

  sealed trait Match

  case class  Ready(players: List[GamePlayer]) extends Match
  case object Terminated extends Match
  case class  DealRound(todo: List[MatchPlayer], done: List[MatchPlayer], remaining: Int, deck: List[Card]) extends Match
  case class  DrawRound(todo: List[MatchPlayer], done: List[MatchPlayer], deck: List[Card], trump: Card) extends Match
  case class  PlayRound(todo: List[MatchPlayer], done: List[(MatchPlayer, Card)], deck: List[Card], trump: Card) extends Match

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
    def deal(f: (Card, List[Card]) => (Match, List[Event])): (Match, List[Event]) =
      deck match
        case card :: restOfTheDeck => f(card, restOfTheDeck)
        case Nil => Terminated -> List(EmptyDeckError, GameAborted)

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
      .scan[(Match, List[Event])](Ready(room.players.map(p => GamePlayer(p, 0))) -> Nil) {

        case ((Ready(players), _), ShuffleDeck(seed)) =>
          val shuffledDeck = new Random(seed).shuffle(Deck.instance)
          val strippedDeck = if (room.players.size == 3) shuffledDeck.filterNot(_ == Card(Rank.Due, Suit.Coppe)) else shuffledDeck
          DealRound(players.map(MatchPlayer(_, Set.empty, Set.empty)), Nil, 2, strippedDeck) ->
            List(DeckShuffled(seed))

        case ((DealRound(player :: Nil, done, 0, deck), _), DrawCard(p)) if player.is(p) =>
          deck.deal {
            case (card, trump :: tail) =>
              PlayRound(done :+ player.draw(card), Nil, tail :+ trump, trump) ->
                List(CardDealt(player.id, card), TrumpRevealed(trump))
            case _ =>
              Terminated -> List(EmptyDeckError)
          }

        case ((DealRound(player :: Nil, done, remaining, deck), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => DealRound(done :+ player.draw(card), Nil, remaining - 1, tail) -> List(CardDealt(player.id, card)) }

        case ((DealRound(player :: todo, done, remaining, deck), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => DealRound(todo, done :+ player.draw(card), remaining, tail) -> List(CardDealt(player.id, card)) }

        case ((DrawRound(player :: Nil, done, deck, trump), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => PlayRound(done :+ player.draw(card), Nil, tail, trump) -> List(CardDealt(player.id, card)) }

        case ((DrawRound(player :: todo, done, deck, trump), _), DrawCard(p)) if player.is(p) =>
          deck.deal { (card, tail) => DrawRound(todo, done :+ player.draw(card), tail, trump) -> List(CardDealt(player.id, card)) }

        case ((PlayRound(player :: Nil, done, Nil, trump), _), PlayCard(p, card)) if player.is(p) && player.has(card) =>
          val updatedPlayers = completeTrick(done :+ player.play(card), trump)

          val events = List(
            CardPlayed(player.id, card),
            TrickWinner(updatedPlayers.head.id)
          )

          if (player.hand.size > 1) {
            PlayRound(updatedPlayers, Nil, Nil, trump) -> events
          }
          else {
            val finalEvents = updatedPlayers.map(p => PointsCount(p.id, p.points)) :+ (updatedPlayers.winners match {
              case List(winner) => MatchWinner(winner.id)
              case best => MatchDraw(best.map(_.id))
            })

            Ready(updatedPlayers.map(_.gamePlayer)) -> (events ++ finalEvents)
          }

        case ((PlayRound(player :: Nil, done, deck, trump), _), PlayCard(p, card)) if player.is(p) && player.has(card) =>
          val updatedPlayers = completeTrick(done :+ player.play(card), trump)

          DrawRound(updatedPlayers, Nil, deck, trump) -> List(
            CardPlayed(player.id, card),
            TrickWinner(updatedPlayers.head.id)
          )

        case ((PlayRound(player :: players, done, deck, trump), _), PlayCard(p, card)) if player.is(p) && player.has(card) =>
          PlayRound(players, done :+ player.play(card), deck, trump) -> List(CardPlayed(player.id, card))

        case ((m, _), _) => m -> Nil

      }
      .flatMap { case (_, events) => fs2.Stream.iterable[F, Event](events).map(Message(room.id, _)) }
