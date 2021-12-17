package bastoni.domain.model

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec

sealed trait CardView:
  def value: Option[Card]

case class CardPlayerView(card: Option[Card]) extends CardView:
  override def value: Option[Card] = card

case class CardServerView(card: Card, facing: Direction) extends CardView:
  override def value: Option[Card] = Some(card)
  def toPlayerView(me: PlayerId, context: Option[PlayerId]) = facing match {
    case Direction.Up => CardPlayerView(Some(card))
    case Direction.Down => CardPlayerView(None)
    case _ => CardPlayerView(Option.when(context.contains(me))(card))
  }

object CardPlayerView:
  given Decoder[CardPlayerView] = Decoder[Option[Card]].map(CardPlayerView(_))
  given Encoder[CardPlayerView] = Encoder[Option[Card]].contramap(_.card)

object CardServerView:
  given Codec[CardServerView] = deriveCodec
