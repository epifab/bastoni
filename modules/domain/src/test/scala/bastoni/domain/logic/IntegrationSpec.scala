package bastoni.domain.logic

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.Continue
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.ToPlayer.*
import bastoni.domain.view.{FromPlayer, TableView, ToPlayer}
import cats.effect.syntax.all.*
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Sync}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class IntegrationSpec extends AnyFreeSpec with Matchers:

  extension (player: Player)
    def dumb[F[_]: Sync](sub: GameSubscriber[F], pub: GamePublisher[F]): fs2.Stream[F, Unit] = DumbPlayer(player, roomId, sub, pub)

  val roomId = RoomId.newId

  def playGame(
    numberOfPlayers: 2 | 3 | 4,
    gameType: GameType,
    realSpeed: Boolean = false,
    extraMessages: fs2.Stream[IO, FromPlayer] = fs2.Stream.empty
  ): Event = {
    (for {
      messageBus <- fs2.Stream.eval(MessageBus.inMemory[IO])
      tableBus <- fs2.Stream.eval(TableBus.inMemory[IO])
      gameRepo <- fs2.Stream.eval(JsonRepos.gameRepo)
      roomRepo <- fs2.Stream.eval(JsonRepos.roomRepo)
      tableRepo <- fs2.Stream.eval(JsonRepos.tableRepo)
      messageRepo <- fs2.Stream.eval(JsonRepos.messageRepo)

      gamePub = GameBus.publisher(messageBus)
      gameSub = GameBus.subscriber(tableBus)

      dumbPlayer1 = player1.dumb(gameSub, gamePub)
      dumbPlayer2 = player2.dumb(gameSub, gamePub)
      dumbPlayer3 = player3.dumb(gameSub, gamePub)
      dumbPlayer4 = player4.dumb(gameSub, gamePub)

      playStreams = numberOfPlayers match
        case 2 => dumbPlayer1.concurrently(dumbPlayer2)
        case 3 => dumbPlayer1.concurrently(dumbPlayer2).concurrently(dumbPlayer3)
        case 4 => dumbPlayer1.concurrently(dumbPlayer2).concurrently(dumbPlayer3).concurrently(dumbPlayer4)

      activateStream = (fs2.Stream(ActivateRoom(gameType)).delayBy[IO](500.millis) ++ extraMessages)
        .through(gamePub.publish(player1, roomId))

      gameServiceRunner <- fs2.Stream.resource(
        if (realSpeed) GameService.runner(messageBus, gameRepo, messageRepo)
        else GameService.runner(messageBus, gameRepo, messageRepo, _ => 2.millis)
      )

      gameBusRunner <- fs2.Stream.resource(GameBus.runner(messageBus, tableBus, tableRepo))

      lobbyRunner <- fs2.Stream.resource(Lobby.runner(messageBus, roomRepo))

      lastMessage <-
        messageBus.subscribe
          .concurrently(messageBus.run)
          .concurrently(tableBus.run)
          .concurrently(gameServiceRunner)
          .concurrently(gameBusRunner)
          .concurrently(lobbyRunner)
          .concurrently(activateStream)
          .concurrently(playStreams)
          .evalTap(message => IO(println(message.data.getClass.getSimpleName)))
          .collect[Event] {
            case Message(_, `roomId`, e: Event.GameCompleted) => e
            case Message(_, `roomId`, Event.GameAborted) => Event.GameAborted
          }
          .take(1)
          .interruptAfter(3.seconds)
    } yield lastMessage).compile.lastOrError.unsafeRunSync()
  }

  "Two players can play an entire briscola game" in {
    playGame(2, GameType.Briscola) shouldBe a[Event.GameCompleted]
  }

  "Three players can play an entire briscola game" in {
    playGame(3, GameType.Briscola) shouldBe a[Event.GameCompleted]
  }

  "Four players can play an entire briscola game" in {
    playGame(4, GameType.Briscola) shouldBe a[Event.GameCompleted]
  }

  "Two players can play an entire tressette game" in {
    playGame(2, GameType.Tressette) shouldBe a[Event.GameCompleted]
  }

  "Four players can play an entire tressette game" in {
    playGame(4, GameType.Tressette) shouldBe a[Event.GameCompleted]
  }

  "Aborting a game" in {
    playGame(
      4,
      GameType.Tressette,
      realSpeed = true,
      extraMessages = fs2.Stream.awakeEvery[IO](2.seconds).map(_ => LeaveRoom)
    ) shouldBe Event.GameAborted
  }
