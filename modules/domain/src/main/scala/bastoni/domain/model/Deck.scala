package bastoni.domain.model

import bastoni.domain.model
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import scala.util.Random

case class Deck(cards: List[VisibleCard]):
  def deal1OrDie[T](f: (VisibleCard, Deck) => T): T =
    cards match
      case card :: restOfTheDeck => f(card, new Deck(restOfTheDeck))
      case _ => throw new IllegalStateException("The deck was empty")

  def dealOrDie[T](n: Int)(f: (List[VisibleCard], Deck) => T): T =
    if (cards.size < n) throw new IllegalStateException("The deck was empty")
    else {
      val (dealing, restOfCards) = cards.splitAt(n)
      f(dealing, new Deck(restOfCards))
    }

  def size: Int = cards.size
  def isEmpty: Boolean = cards.isEmpty
  def nonEmpty: Boolean = cards.nonEmpty

  def discard(rank: Rank, suit: Suit): Deck = Deck(cards.filterNot(c => c.rank == rank && c.suit == suit))
  def append(card: VisibleCard): Deck = Deck(cards :+ card)


object Deck:
  val cards: List[SimpleCard] = (for {
    rank <- Rank.values
    suit <- Suit.values
  } yield SimpleCard(rank, suit)).toList

  def shuffled(seed: Int): Deck = new Random(seed).shuffle(cards).toDeck

  given Encoder[Deck] = Encoder[List[VisibleCard]].contramap(_.cards)
  given Decoder[Deck] = Decoder[List[VisibleCard]].map(Deck(_))


extension (cards: List[SimpleCard])
  def toDeck: Deck = Deck(cards.zipWithIndex.map { case (c, ref) => VisibleCard(c.rank, c.suit, CardId(ref)) })
