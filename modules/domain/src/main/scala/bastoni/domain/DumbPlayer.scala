package bastoni.domain

import bastoni.domain.logic.{GamePublisher, GameSubscriber}
import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.ToPlayer
import cats.effect.syntax.temporal.*
import cats.effect.{Sync, Temporal}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.chaining.*

object DumbPlayer:
  def apply[F[_]: Sync: Temporal](me: Player, roomId: RoomId, subscriber: GameSubscriber[F], publisher: GamePublisher[F], pause: FiniteDuration = 0.millis): fs2.Stream[F, Unit] =
    val actions =
      fs2.Stream(Connect, JoinTable) ++ subscriber
        .subscribe(me, roomId)
        .scan(Option.empty[TablePlayerView]) {
          case (_, ToPlayer.Snapshot(table)) => Some(table)
          case (table, ToPlayer.GameEvent(event)) => table.map(_.update(event))
        }
        .collect { case Some(table) => table }
        .map(
          _.seatFor(me) match {
            case Some(PlayerSeat(ActingPlayer(_, Action.PlayCard), hand, _, _)) =>
              Some(PlayCard(hand.flatMap(_.card).headOption.getOrElse(throw new IllegalStateException("No cards in hand"))))

            case Some(PlayerSeat(ActingPlayer(_, Action.PlayCardOf(suit)), hand, _, _)) =>
              Some(PlayCard(hand.flatMap(_.card).pipe(hand => hand.find(_.suit == suit).orElse(hand.headOption)).getOrElse(throw new IllegalStateException("No cards in hand"))))

            case Some(PlayerSeat(ActingPlayer(_, Action.ShuffleDeck), _, _, _)) =>
              Some(ShuffleDeck)

            case _ => None
          }
        )
        .collect { case Some(event) => event }
        .evalMap { event => Sync[F].pure(event).delayBy(pause) }

    actions
      // .evalTap { act => Sync[F].delay(println(s"${me.name} will ${act.getClass.getSimpleName.filter(_ != '$')}")) }
      .through(publisher.publish(me, roomId))
