package bastoni.domain

enum Suit:
  case Denari, Coppe, Spade, Bastoni

enum Rank(val value: Int):
  case Asso extends Rank(1)
  case Due extends Rank(2)
  case Tre extends Rank(3)
  case Quattro extends Rank(4)
  case Cinque extends Rank(5)
  case Sei extends Rank(6)
  case Sette extends Rank(7)
  case Donna extends Rank(8)
  case Fante extends Rank(9)
  case Re extends Rank(10)

case class Card(suit: Suit, rank: Rank)

type Deck = Set[Card]

object Deck:
  val instance: Set[Card] = (for {
    suit <- Suit.values
    rank <- Rank.values
  } yield Card(suit, rank)).toSet
