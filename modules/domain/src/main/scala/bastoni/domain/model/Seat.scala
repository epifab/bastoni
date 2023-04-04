package bastoni.domain.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait Seat[C <: CardView]:
  def index: Int
  def hand: List[C]
  def taken: List[C]
  def playerOption: Option[PlayerState]
  def occupiedBy(player: PlayerState): TakenSeat[C] = TakenSeat(index, player, hand, taken)
  def vacant: EmptySeat[C]                          = EmptySeat(index, hand, taken)

case class EmptySeat[C <: CardView](index: Int, hand: List[C], taken: List[C]) extends Seat[C]:
  override val playerOption: Option[PlayerState] = None

case class TakenSeat[C <: CardView](index: Int, player: PlayerState, hand: List[C], taken: List[C]) extends Seat[C]:
  override val playerOption: Option[PlayerState] = Some(player)

object Seat:
  given [C <: CardView: Decoder]: Decoder[Seat[C]] = Decoder.instance { cursor =>
    for
      emptySeat <- deriveDecoder[EmptySeat[C]].tryDecode(cursor)
      player    <- cursor.downField("player").as[Option[PlayerState]]
    yield player.fold(emptySeat)(emptySeat.occupiedBy)
  }

  given [C <: CardView: Encoder]: Encoder[Seat[C]] = Encoder.instance {
    case seat: TakenSeat[C] => deriveEncoder[TakenSeat[C]](seat)
    case seat: EmptySeat[C] => deriveEncoder[EmptySeat[C]](seat)
  }
