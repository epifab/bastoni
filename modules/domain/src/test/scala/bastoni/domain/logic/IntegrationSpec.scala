package bastoni.domain
package logic

import bastoni.domain.ai.DumbPlayer
import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.Continue
import bastoni.domain.model.Event.GameAborted
import bastoni.domain.repos.{GameRepo, MessageRepo}
import bastoni.domain.view.{FromPlayer, ToPlayer}
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.ToPlayer.*
import cats.effect.{IO, Sync}
import cats.effect.syntax.all.*
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt

class IntegrationSpec extends AsyncIOFreeSpec:
  // on scalajs tests run very slow, possibly due to the single threaded EC
  private val timeout = 30.seconds

  extension (player: User)
    def dumb(controller: GameController[IO]): fs2.Stream[IO, Unit] =
      DumbPlayer(controller).play(player, roomId)

  val roomId: RoomId = RoomId.newId

  def playGame(
      numberOfPlayers: 2 | 3 | 4,
      gameType: GameType,
      realSpeed: Boolean = false,
      extraMessages: fs2.Stream[IO, GameCommand] = fs2.Stream.empty
  ): IO[Event] =
    (for
      messageBus   <- fs2.Stream.eval(MessageBus.inMemory[IO])
      messageQueue <- fs2.Stream.resource(MessageQueue.inMemory(messageBus))
      gameRepo     <- fs2.Stream.eval(GameRepo.inMemory[IO])
      messageRepo  <- fs2.Stream.eval(MessageRepo.inMemory[IO])

      controller = GameController(messageBus)

      dumbPlayer1 = user1.dumb(controller)
      dumbPlayer2 = user2.dumb(controller)
      dumbPlayer3 = user3.dumb(controller)
      dumbPlayer4 = user4.dumb(controller)

      playStreams = numberOfPlayers match
        case 2 => dumbPlayer1.concurrently(dumbPlayer2)
        case 3 => dumbPlayer1.concurrently(dumbPlayer2).concurrently(dumbPlayer3)
        case 4 => dumbPlayer1.concurrently(dumbPlayer2).concurrently(dumbPlayer3).concurrently(dumbPlayer4)

      activateStream = (fs2.Stream(StartMatch(gameType)).delayBy[IO](500.millis) ++ extraMessages)
        .through(controller.publish(user1, roomId))

      gameServiceRunner = GameService.runner(
        name = "test",
        messageQueue = messageQueue,
        messageBus = messageBus,
        gameRepo = gameRepo,
        messageRepo = messageRepo,
        delayDuration =
          if realSpeed then Delay.default
          else {
            case Delay.ActionTimeout => 100.millis
            case _                   => 2.millis
          }
      )

      lastMessage <-
        fs2.Stream
          .resource(messageBus.subscribe)
          .flatMap(stream =>
            stream
              .concurrently(messageBus.run)
              .concurrently(messageQueue.run)
              .concurrently(gameServiceRunner)
              .concurrently(playStreams)
              .concurrently(activateStream)
              .collect[Event] {
                case Message(_, `roomId`, e: Event.MatchCompleted)    => e
                case Message(_, `roomId`, Event.MatchAborted(reason)) => Event.MatchAborted(reason)
              }
              .take(1)
              .interruptAfter(timeout)
          )
    yield lastMessage).compile.last.flatMap {
      case Some(message) => IO.pure(message)
      case None          => IO.raiseError(fail(s"This game did not terminate within $timeout"))
    }

  "Two players can play an entire briscola game" in {
    playGame(2, GameType.Briscola).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Three players can play an entire briscola game" in {
    playGame(3, GameType.Briscola).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Four players can play an entire briscola game" in {
    playGame(4, GameType.Briscola).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Two players can play an entire tressette game" in {
    playGame(2, GameType.Tressette).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Four players can play an entire tressette game" in {
    playGame(4, GameType.Tressette).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Two players can play an entire scopa game" in {
    playGame(2, GameType.Scopa).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Three players can play an entire scopa game" in {
    playGame(2, GameType.Scopa).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Four players can play an entire scopa game" in {
    playGame(4, GameType.Scopa).asserting(_ shouldBe a[Event.MatchCompleted])
  }

  "Aborting a game" in {
    playGame(
      4,
      GameType.Tressette,
      realSpeed = true,
      extraMessages = fs2.Stream(LeaveTable).delayBy(2.seconds)
    ).asserting(_ shouldBe Event.MatchAborted(GameAborted.Reason.playerLeftTheRoom))
  }
end IntegrationSpec
