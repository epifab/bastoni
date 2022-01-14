package bastoni.domain

import bastoni.domain.logic.{GamePublisher, GameSubscriber}
import bastoni.domain.model.*
import bastoni.domain.model.PlayerState.*
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.syntax.temporal.*
import cats.effect.{Sync, Temporal}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.chaining.*

object DumbPlayer:
  def apply[F[_]: Sync: Temporal](me: User, roomId: RoomId, subscriber: GameSubscriber[F], publisher: GamePublisher[F], pause: FiniteDuration = 0.millis): fs2.Stream[F, Unit] =
    val actions: fs2.Stream[F, FromPlayer] =
      fs2.Stream(Connect, JoinTable) ++ subscriber
        .subscribe(me, roomId)
        .zipWithScan1(Option.empty[TablePlayerView]) {
          case (_, ToPlayer.Snapshot(table)) => Some(table)
          case (table, ToPlayer.GameEvent(event)) => table.map(_.update(event))
        }
        .collect {
          case (ToPlayer.GameEvent(Event.ActionRequested(playerId, _, _)), Some(table)) if me.is(playerId) =>
            table -> table.seatFor(me).getOrElse(throw new IllegalStateException("I am not sat at this table"))
        }
        .collect {
          case (table, TakenSeat(ActingPlayer(_, Action.PlayCard, _), hand, _)) =>
            PlayCard(hand.flatMap(_.card.toOption).headOption.getOrElse(throw new IllegalStateException("No cards in hand")))

          case (table, TakenSeat(ActingPlayer(_, Action.PlayCardOf(suit), _), hand, _)) =>
            PlayCard(hand.flatMap(_.card.toOption).pipe(hand => hand.find(_.suit == suit).orElse(hand.headOption)).getOrElse(throw new IllegalStateException("No cards in hand")))

          case (table, TakenSeat(ActingPlayer(_, Action.TakeCards, _), hand, _)) =>
            val cardToPlay = hand.flatMap(_.card.toOption).headOption.getOrElse(throw new IllegalStateException("No cards in hand"))
            val takes = bastoni.domain.logic.scopa.Game.takeCombinations(table.board.flatMap { case (_, c) => c.card.toOption }, cardToPlay).next()
            TakeCards(cardToPlay, takes.toList)

          case (table, TakenSeat(ActingPlayer(_, Action.ShuffleDeck, _), _, _)) =>
            ShuffleDeck
        }
        .evalMap(command => Sync[F].pure(command).delayBy(pause))

    actions
      // .evalTap { act => Sync[F].delay(println(s"${me.name} will ${act.getClass.getSimpleName.filter(_ != '$')}")) }
      .through(publisher.publish(me, roomId))
