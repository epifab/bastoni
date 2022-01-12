package bastoni.domain.model

import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
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

sealed trait Card:
  def rank: Rank
  def suit: Suit

sealed trait CardRef:
  def ref: Int

case class SimpleCard(rank: Rank, suit: Suit) extends Card

object SimpleCard:
  given Encoder[SimpleCard] = deriveEncoder
  given Decoder[SimpleCard] = deriveDecoder

sealed trait CardInstance extends CardRef:
  def toOption: Option[VisibleCard]
  def isHidden: Boolean = toOption.isEmpty
  def contains(card: SimpleCard): Boolean = toOption.map(_.simple).contains(card)
  def hidden: HiddenCard = HiddenCard(ref)

case class VisibleCard(rank: Rank, suit: Suit, ref: Int) extends CardInstance with Card:
  override def toOption: Option[VisibleCard] = Some(this)
  def simple: SimpleCard = SimpleCard(rank, suit)

object VisibleCard:
  given Encoder[VisibleCard] = deriveEncoder
  given Decoder[VisibleCard] = deriveDecoder

case class HiddenCard(ref: Int) extends CardInstance:
  override def toOption: Option[VisibleCard] = None

object HiddenCard:
  given Encoder[HiddenCard] = deriveEncoder
  given Decoder[HiddenCard] = deriveDecoder

object Card:
  def apply(rank: Rank, suit: Suit): SimpleCard = SimpleCard(rank, suit)
  def apply(rank: Rank, suit: Suit, ref: Int): VisibleCard = VisibleCard(rank, suit, ref)
  def apply(ref: Int): HiddenCard = HiddenCard(ref)

  given Encoder[CardInstance] = deriveEncoder
  given Decoder[CardInstance] = deriveDecoder
