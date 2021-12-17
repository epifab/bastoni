package bastoni.domain.model

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
