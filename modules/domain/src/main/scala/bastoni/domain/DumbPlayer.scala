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
      fs2.Stream(Connect, JoinRoom) ++ subscriber
        .subscribe(me, roomId)
        .zipWithScan1(Option.empty[RoomPlayerView]) {
          case (_, ToPlayer.Snapshot(room)) => Some(room)
          case (room, ToPlayer.Request(request)) => room.map(_.withRequest(request))
          case (room, ToPlayer.GameEvent(event)) => room.map(_.update(event))
        }
        .collect {
          case (ToPlayer.Request(Command.Act(playerId, _, _)), Some(room)) if me.is(playerId) =>
            room -> room.seatFor(me).getOrElse(throw new IllegalStateException("I am not sat at this table"))
        }
        .collect {
          case (room, TakenSeat(_, ActingPlayer(_, Action.PlayCard, _), hand, _)) =>
            PlayCard(hand.flatMap(_.card.toOption).headOption.getOrElse(throw new IllegalStateException("No cards in hand")))

          case (room, TakenSeat(_, ActingPlayer(_, Action.PlayCardOf(suit), _), hand, _)) =>
            PlayCard(hand.flatMap(_.card.toOption).pipe(hand => hand.find(_.suit == suit).orElse(hand.headOption)).getOrElse(throw new IllegalStateException("No cards in hand")))

          case (room, TakenSeat(_, ActingPlayer(_, Action.TakeCards, _), hand, _)) =>
            val cardToPlay = hand.flatMap(_.card.toOption).headOption.getOrElse(throw new IllegalStateException("No cards in hand"))
            val takes = bastoni.domain.logic.scopa.Game.takeCombinations(room.board.flatMap { case (_, c) => c.card.toOption }, cardToPlay).next()
            TakeCards(cardToPlay, takes.toList)

          case (room, TakenSeat(_, ActingPlayer(_, Action.ShuffleDeck, _), _, _)) => ShuffleDeck
        }
        .evalMap(command => Sync[F].pure(command).delayBy(pause))

    actions
      // .evalTap { act => Sync[F].delay(println(s"${me.name} will ${act.getClass.getSimpleName.filter(_ != '$')}")) }
      .through(publisher.publish(me, roomId))
