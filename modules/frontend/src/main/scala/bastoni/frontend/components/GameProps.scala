package bastoni.frontend.components

import bastoni.domain.model.*

import scala.util.chaining.*

extension[T](list: List[T])
  def get(index: Int): Option[T] =
    list match {
      case Nil => None
      case head :: tail if index == 0 => Some(head)
      case head :: tail => tail.get(index - 1)
    }

case class GameProps(table: TablePlayerView, me: UserId):
  val indexedSeats: List[(Seat[CardPlayerView], Int)] =
    table.seats.slideUntil(_.player.exists(_.is(me)))
      .zipWithIndex
      .map { case (seat, zeroBasedIndex) => seat -> (zeroBasedIndex + 1) }

  val opponents: List[(Seat[CardPlayerView], Int)] = indexedSeats.filterNot { case (seat, index) => seat.player.exists(_.is(me)) }
  def opponent(index: Int): Option[Seat[CardPlayerView]] = opponents.get(index).map(_._1)

  val mySeat: Option[TakenSeat[CardPlayerView]] = table.seatFor(me)
