package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer.{JoinRoom, PlayCard, ShuffleDeck}
import bastoni.domain.view.{PlayerSeat, SeatView, TableView, ToPlayer}
import cats.effect.{Sync}

import scala.util.chaining.*

object DumbPlayer:
  def apply[F[_]: Sync](me: Player, roomId: RoomId, subscriber: GameSubscriber[F], publisher: GamePublisher[F]): fs2.Stream[F, Unit] =
    val actions =
      fs2.Stream(JoinRoom) ++ subscriber
        .subscribe(me, roomId)
        .map { case ToPlayer.Snapshot(table) =>
          table.seatFor(me) match {
            case Some(PlayerSeat(ActingPlayer(_, Action.PlayCard), hand, _, _)) =>
              Some(PlayCard(hand.flatten.headOption.getOrElse(throw new IllegalStateException("No cards in hand"))))

            case Some(PlayerSeat(ActingPlayer(_, Action.PlayCardOf(suit)), hand, _, _)) =>
              Some(PlayCard(hand.flatten.pipe(hand => hand.find(_.suit == suit).orElse(hand.headOption)).getOrElse(throw new IllegalStateException("No cards in hand"))))

            case Some(PlayerSeat(ActingPlayer(_, Action.ShuffleDeck), _, _, _)) =>
              Some(ShuffleDeck)

            case _ => None
          }
        }
        .collect { case Some(event) => event }

    actions
      .evalTap { act => Sync[F].delay(println(s"${me.name} will ${act.getClass.getSimpleName.filter(_ != '$')}")) }
      .through(publisher.publish(me, roomId))
