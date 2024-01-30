package bastoni.domain.ai

import bastoni.domain.logic.{GameController, GamePublisher, GameSubscriber}
import bastoni.domain.model.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import bastoni.domain.view.FromPlayer.{Connect, JoinTable}
import cats.effect.{Sync, Temporal}
import cats.effect.kernel.Async
import cats.effect.syntax.temporal.*
import cats.syntax.all.*
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

case class ActContext(
    matchInfo: MatchInfo,
    room: Room[CardPlayerView],
    mySeat: OccupiedSeat[CardPlayerView]
)

trait ActStrategy:
  def act(context: ActContext, action: Action): FromPlayer.GameCommand

class VirtualPlayer[F[_]: Async: Logger](
    controller: GameController[F],
    strategy: ActStrategy,
    pause: FiniteDuration = 0.millis
):
  def play(me: User, roomId: RoomId): fs2.Stream[F, Unit] =
    val actions = fs2.Stream(Connect, JoinTable) ++ controller
      .subscribe(me, roomId)
      .zipWithScan1(Option.empty[RoomPlayerView]) {
        case (_, ToPlayer.Connected(room))     => Some(room)
        case (_, ToPlayer.Disconnected(_))     => None
        case (room, ToPlayer.Request(request)) => room.map(_.withRequest(request))
        case (room, ToPlayer.GameEvent(event)) => room.map(_.update(event))
        case (room, ToPlayer.Authenticated(_)) => room
        case (room, ToPlayer.Ping)             => room
      }
      .map { case (message, maybeRoom) =>
        for
          room      <- maybeRoom
          matchInfo <- room.matchInfo
          seat      <- room.seatFor(me)
        yield (ActContext(matchInfo, room, seat), message)
      }
      .collect {
        case Some((context, ToPlayer.Request(Command.Act(playerId, action, _)))) if me.is(playerId) =>
          strategy.act(context, action)
      }
      .evalMap(command =>
        Logger[F].debug(Console.CYAN + show"VirtualPlayer: $me will play $command" + Console.RESET) *>
          Sync[F].pure(command).delayBy(pause)
      )

    actions.through(controller.publish(me, roomId))
end VirtualPlayer
