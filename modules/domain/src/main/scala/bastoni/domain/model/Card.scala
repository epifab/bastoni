package bastoni.domain.model

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
  case Fante extends Rank(8)
  case Cavallo extends Rank(9)
  case Re extends Rank(10)

case class Card(rank: Rank, suit: Suit)

object Deck:
  val instance: List[Card] = (for {
    rank <- Rank.values
    suit <- Suit.values
  } yield Card(rank, suit)).toList

extension(deck: List[Card])
  def dealOrDie[T](f: (Card, List[Card]) => T): T =
    deck match
      case card :: restOfTheDeck => f(card, restOfTheDeck)
      case _ => throw new RuntimeException("The deck was empty")
