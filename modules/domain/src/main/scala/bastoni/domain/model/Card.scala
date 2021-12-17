package bastoni.domain.model

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

enum Suit:
  case Denari, Coppe, Spade, Bastoni

object Suit:
  given Encoder[Suit] = Encoder[String].contramap(_.toString)
  given Decoder[Suit] = Decoder[String].map(Suit.valueOf)

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

object Rank:
  given Encoder[Rank] = Encoder[String].contramap(_.toString)
  given Decoder[Rank] = Decoder[String].map(Rank.valueOf)

case class Card(rank: Rank, suit: Suit)

object Card:
  given Encoder[Card] = Encoder[(Rank, Suit)].contramap(card => card.rank -> card.suit)
  given Decoder[Card] = Decoder[(Rank, Suit)].map { case (rank, suit) => Card(rank, suit) }
