package bastoni.domain
package logic

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.Continue
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.ToPlayer.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.syntax.all.*
import cats.effect.{IO, Sync}

import scala.concurrent.duration.DurationInt

class IntegrationSpec extends AsyncIOFreeSpec:
  // on scalajs tests run very slow, possibly due to the single threaded EC
  private val timeout = 30.seconds

  extension (player: User)
    def dumb(sub: GameSubscriber[IO], pub: GamePublisher[IO]): fs2.Stream[IO, Unit] = DumbPlayer(player, roomId, sub, pub)

  val roomId = RoomId.newId

  def playGame(
    numberOfPlayers: 2 | 3 | 4,
    gameType: GameType,
    realSpeed: Boolean = false,
    extraMessages: fs2.Stream[IO, FromPlayer] = fs2.Stream.empty
  ): IO[Event] = {
    (for {
      messageBus <- fs2.Stream.eval(MessageBus.inMemory[IO])
      gameRepo <- fs2.Stream.eval(JsonRepos.gameRepo)
      messageRepo <- fs2.Stream.eval(JsonRepos.messageRepo)

      gamePub = GamePubSub.publisher(messageBus)
      gameSub = GamePubSub.subscriber(messageBus)

      dumbPlayer1 = user1.dumb(gameSub, gamePub)
      dumbPlayer2 = user2.dumb(gameSub, gamePub)
      dumbPlayer3 = user3.dumb(gameSub, gamePub)
      dumbPlayer4 = user4.dumb(gameSub, gamePub)

      playStreams = numberOfPlayers match
        case 2 => dumbPlayer1.concurrently(dumbPlayer2)
        case 3 => dumbPlayer1.concurrently(dumbPlayer2).concurrently(dumbPlayer3)
        case 4 => dumbPlayer1.concurrently(dumbPlayer2).concurrently(dumbPlayer3).concurrently(dumbPlayer4)

      activateStream = (fs2.Stream(StartGame(gameType)).delayBy[IO](500.millis) ++ extraMessages)
        .through(gamePub.publish(user1, roomId))

      gameServiceRunner <- fs2.Stream.resource(
        if (realSpeed) GameService.runner(messageBus, gameRepo, messageRepo)
        else GameService.runner(messageBus, gameRepo, messageRepo, _ => 2.millis)
      )

      lastMessage <-
        messageBus
          .subscribe
          .concurrently(messageBus.run)
          .concurrently(gameServiceRunner)
          .concurrently(activateStream)
          .concurrently(playStreams)
          // .evalTap(message => IO(println(message.data.getClass.getSimpleName)))
          .collect[Event] {
            case Message(_, `roomId`, e: Event.MatchCompleted) => e
            case Message(_, `roomId`, Event.MatchAborted) => Event.MatchAborted
          }
          .take(1)
          .interruptAfter(timeout)
    } yield lastMessage).compile.lastOrError
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

  "Aborting a game" in {
    playGame(
      4,
      GameType.Tressette,
      realSpeed = true,
      extraMessages = fs2.Stream.awakeEvery[IO](2.seconds).map(_ => LeaveTable)
    ).asserting(_ shouldBe Event.MatchAborted)
  }
