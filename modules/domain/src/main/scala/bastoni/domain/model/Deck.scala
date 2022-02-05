package bastoni.domain.model

import bastoni.domain.model
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.util.Random

opaque type Deck = List[VisibleCard]

object Deck:
  extension (cards: Deck)
    def deal1OrDie[T](f: (VisibleCard, Deck) => T): T =
      cards match
        case card :: restOfTheDeck => f(card, restOfTheDeck)
        case _ => throw new IllegalStateException("The deck was empty")

    def dealOrDie[T](n: Int)(f: (List[VisibleCard], Deck) => T): T =
      if (cards.size < n) throw new IllegalStateException("The deck was empty")
      else {
        val (dealing, restOfCards) = cards.splitAt(n)
        f(dealing, restOfCards)
      }

    def size: Int = cards.size
    def isEmpty: Boolean = cards.isEmpty
    def nonEmpty: Boolean = cards.nonEmpty
    def asList: List[VisibleCard] = cards
    def map[A](f: VisibleCard => A): List[A] = cards.map(f)
    def discard(rank: Rank, suit: Suit): Deck = cards.filterNot(c => c.rank == rank && c.suit == suit)
    def append(card: VisibleCard): Deck = cards :+ card


  val cards: List[SimpleCard] = (for {
    rank <- Rank.values
    suit <- Suit.values
  } yield SimpleCard(rank, suit)).toList

  def apply(cards: VisibleCard*): Deck = cards.toList
  def shuffled(seed: Int): Deck = new Random(seed).shuffle(cards).toDeck

  given Encoder[Deck] = Encoder.encodeList[VisibleCard]
  given Decoder[Deck] = Decoder.decodeList[VisibleCard]


extension (cards: List[SimpleCard])
  def toDeck: Deck = cards.zipWithIndex.map { case (c, ref) => VisibleCard(c.rank, c.suit, CardId(ref)) }
