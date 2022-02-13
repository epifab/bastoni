package bastoni.domain.ai

import bastoni.domain.logic.{GamePublisher, GameSubscriber}
import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer.{Connect, JoinRoom}
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.syntax.temporal.*
import cats.effect.{Sync, Temporal}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

case class ActContext(
  matchInfo: MatchInfo,
  room: Room[CardPlayerView],
  seat: TakenSeat[CardPlayerView]
)

trait ActStrategy:
  def act(context: ActContext, action: Action): FromPlayer

object VirtualPlayer:
  def apply[F[_]: Sync: Temporal](me: User, roomId: RoomId, subscriber: GameSubscriber[F], publisher: GamePublisher[F], strategy: ActStrategy, pause: FiniteDuration = 0.millis): fs2.Stream[F, Unit] =
    val actions = fs2.Stream(Connect, JoinRoom) ++ subscriber
      .subscribe(me, roomId)
      .zipWithScan1(Option.empty[RoomPlayerView]) {
        case (_, ToPlayer.Snapshot(room)) => Some(room)
        case (room, ToPlayer.Request(request)) => room.map(_.withRequest(request))
        case (room, ToPlayer.GameEvent(event)) => room.map(_.update(event))
      }
      .map {
        case (message, maybeRoom) =>
          for {
            room <- maybeRoom
            matchInfo <- room.matchInfo
            seat <- room.seatFor(me)
          } yield (ActContext(matchInfo, room, seat), message)
      }
      .collect {
        case Some((context, ToPlayer.Request(Command.Act(playerId, action, _)))) if me.is(playerId) =>
          strategy.act(context, action)
      }
      .evalMap(command => Sync[F].pure(command).delayBy(pause))

    actions.through(publisher.publish(me, roomId))
