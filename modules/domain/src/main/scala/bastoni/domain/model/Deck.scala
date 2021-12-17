package bastoni.domain.model

object Deck:
  val instance: List[Card] = (for {
    rank <- Rank.values
    suit <- Suit.values
  } yield Card(rank, suit)).toList

extension(deck: List[Card])
  def deal1OrDie[T](f: (Card, List[Card]) => T): T =
    deck match
      case card :: restOfTheDeck => f(card, restOfTheDeck)
      case _ => throw new IllegalStateException("The deck was empty")

  def dealOrDie[T](n: Int)(f: (List[Card], List[Card]) => T): T =
    if (deck.size < n) throw new IllegalStateException("The deck was empty")
    else f.tupled(deck.splitAt(n))
