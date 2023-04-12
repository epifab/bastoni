package bastoni.domain.model

import bastoni.domain.model
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

sealed trait CardView:
  def card: CardInstance
  def ref: CardId                   = card.ref
  def toOption: Option[VisibleCard] = card.toOption

case class CardPlayerView(card: CardInstance) extends CardView

case class CardServerView(card: VisibleCard, facing: Direction) extends CardView:

  def toPlayerView(me: UserId, context: Option[UserId]): CardPlayerView = facing match
    case Direction.Up   => CardPlayerView(card)
    case Direction.Down => CardPlayerView(card.hide)
    case _              => CardPlayerView(if (context.contains(me)) card else card.hide)

object CardPlayerView:
  given Encoder[CardPlayerView] = CardInstance.encoder.contramap(_.card)
  given Decoder[CardPlayerView] = CardInstance.decoder.map(card => CardPlayerView(card))

object CardServerView:
  given Encoder[CardServerView] = deriveEncoder
  given Decoder[CardServerView] = deriveDecoder
