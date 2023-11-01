package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait Seat[C <: CardView]:
  def index: Int
  def hand: List[C]
  def pile: List[C]
  def playerOption: Option[PlayerState]
  def occupiedBy(player: PlayerState): OccupiedSeat[C] = OccupiedSeat(index, player, hand, pile)
  def vacant: EmptySeat[C]                             = EmptySeat(index, hand, pile)

case class EmptySeat[C <: CardView](index: Int, hand: List[C], pile: List[C]) extends Seat[C]:
  override val playerOption: Option[PlayerState] = None

case class OccupiedSeat[C <: CardView](index: Int, occupant: PlayerState, hand: List[C], pile: List[C]) extends Seat[C]:
  override val playerOption: Option[PlayerState] = Some(occupant)

object Seat:
  given [C <: CardView: Decoder]: Decoder[Seat[C]] = Decoder.instance { cursor =>
    for
      emptySeat <- deriveDecoder[EmptySeat[C]].tryDecode(cursor)
      player    <- cursor.downField("player").as[Option[PlayerState]]
    yield player.fold(emptySeat)(emptySeat.occupiedBy)
  }

  given [C <: CardView: Encoder]: Encoder[Seat[C]] = Encoder.instance {
    case seat: OccupiedSeat[C] => deriveEncoder[OccupiedSeat[C]](seat)
    case seat: EmptySeat[C]    => deriveEncoder[EmptySeat[C]](seat)
  }
