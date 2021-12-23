package bastoni.frontend.components

import bastoni.domain.model.*

import scala.util.chaining.*

case class GameProps(table: TablePlayerView, me: UserId):
  val indexedSeats: List[(Seat[CardPlayerView], Int)] =
    table.seats.slideUntil(_.player.exists(_.is(me)))
      .zipWithIndex
      .map { case (seat, zeroBasedIndex) => seat -> (zeroBasedIndex + 1) }

  val opponents: List[(Seat[CardPlayerView], Int)] = indexedSeats.filterNot { case (seat, index) => seat.player.exists(_.is(me)) }
  val mySeat: Option[TakenSeat[CardPlayerView]] = table.seatFor(me)
