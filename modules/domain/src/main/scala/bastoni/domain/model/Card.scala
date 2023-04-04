package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

enum Suit:
  case Denari, Coppe, Spade, Bastoni

object Suit:
  given Encoder[Suit] = Encoder[String].contramap(_.toString)
  given Decoder[Suit] = Decoder[String].map(Suit.valueOf)

enum Rank(val value: Int):
  case Asso    extends Rank(1)
  case Due     extends Rank(2)
  case Tre     extends Rank(3)
  case Quattro extends Rank(4)
  case Cinque  extends Rank(5)
  case Sei     extends Rank(6)
  case Sette   extends Rank(7)
  case Fante   extends Rank(8)
  case Cavallo extends Rank(9)
  case Re      extends Rank(10)

object Rank:
  given Encoder[Rank] = Encoder[String].contramap(_.toString)
  given Decoder[Rank] = Decoder[String].map(Rank.valueOf)

sealed trait Card:
  def rank: Rank
  def suit: Suit

opaque type CardId = Int

object CardId:
  def unknown: CardId        = -1
  def apply(id: Int): CardId = id
  given Encoder[CardId]      = Encoder.encodeInt
  given Decoder[CardId]      = Decoder.decodeInt

sealed trait CardRef:
  def ref: CardId

case class SimpleCard(rank: Rank, suit: Suit) extends Card

object SimpleCard:
  given Encoder[SimpleCard] = deriveEncoder
  given Decoder[SimpleCard] = deriveDecoder

sealed trait CardInstance extends CardRef:
  def toOption: Option[VisibleCard]
  def hide: HiddenCard
  def isHidden: Boolean                   = toOption.isEmpty
  def contains(card: SimpleCard): Boolean = toOption.map(_.simple).contains(card)

case class VisibleCard(rank: Rank, suit: Suit, ref: CardId) extends CardInstance with Card:
  def simple: SimpleCard                     = SimpleCard(rank, suit)
  override def toOption: Option[VisibleCard] = Some(this)
  override def hide: HiddenCard              = HiddenCard(ref, Some(simple))

object VisibleCard:
  given Encoder[VisibleCard] = deriveEncoder
  given Decoder[VisibleCard] = deriveDecoder

case class HiddenCard(ref: CardId, card: Option[Card] = None) extends CardInstance:
  override def toOption: Option[VisibleCard] = None
  override def hide: HiddenCard              = this

object HiddenCard:
  given Encoder[HiddenCard] = deriveEncoder
  given Decoder[HiddenCard] = deriveDecoder

object Card:
  def apply(rank: Rank, suit: Suit): SimpleCard               = SimpleCard(rank, suit)
  def apply(rank: Rank, suit: Suit, ref: CardId): VisibleCard = VisibleCard(rank, suit, ref)
  def apply(ref: CardId): HiddenCard                          = HiddenCard(ref)

  given Encoder[CardInstance] = deriveEncoder
  given Decoder[CardInstance] = deriveDecoder
