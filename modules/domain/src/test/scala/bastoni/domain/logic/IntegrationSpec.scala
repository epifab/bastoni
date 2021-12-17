package bastoni.domain.logic

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.Continue
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.ToPlayer.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

object DumbPlayer:
  def apply[F[_]](me: Player, roomId: RoomId, gameBus: GameBus[F]): fs2.Stream[F, Unit] =
    gameBus.publish(me, roomId, fs2.Stream(JoinRoom) ++
      gameBus
        .subscribe(me, roomId)
        .zipWithScan(Option.empty[MatchPlayer]) {
          case (None, GameStarted(_)) =>
            Some(MatchPlayer(GamePlayer(me, 0), Set.empty, Set.empty))
          case (Some(player), CardDealt(playerId, Some(card))) if player.is(playerId) =>
            Some(player.draw(card))
          case (Some(player), CardPlayed(playerId, card)) if player.is(playerId) =>
            Some(player.play(card)._1)
          case (whatever, _) => whatever
        }
        .collect {
          case (ActionRequest(playerId, Command.Action.PlayCard), Some(player)) if player.is(playerId) => PlayCard(player.hand.head)
          case (ActionRequest(playerId, Command.Action.PlayCardOf(suit)), Some(player)) if player.is(playerId) => PlayCard(player.hand.find(_.suit == suit).getOrElse(player.hand.head))
          case (ActionRequest(playerId, Command.Action.ShuffleDeck), Some(player)) if player.is(playerId) => ShuffleDeck
        }
    )


class IntegrationSpec extends AnyFreeSpec with Matchers:

  extension (player: Player)
    def dumb[F[_]](gameBus: GameBus[F]): fs2.Stream[F, Unit] = DumbPlayer(player, roomId, gameBus)

  val roomId = RoomId.newId

  def playGame(
    numberOfPlayers: 2 | 3 | 4,
    gameType: GameType,
    realSpeed: Boolean = false,
    extraMessages: fs2.Stream[IO, FromPlayer] = fs2.Stream.empty
  ): Event = {
    (for {
      bus <- MessageBus.inMemory[IO]
      gameRepo <- JsonRepos.gameRepo
      roomRepo <- JsonRepos.roomRepo
      messageRepo <- JsonRepos.messageRepo
      gameBus = GameBus(bus)

      twoPlayers = player1.dumb(gameBus).concurrently(player2.dumb(gameBus))
      threePlayers = twoPlayers.concurrently(player3.dumb(gameBus))
      fourPlayers = threePlayers.concurrently(player4.dumb(gameBus))

      playStreams = numberOfPlayers match
        case 2 => twoPlayers
        case 3 => threePlayers
        case 4 => fourPlayers

      activateStream = gameBus.publish(
        player1,
        roomId,
        fs2.Stream(ActivateRoom(gameType)).delayBy[IO](500.millis) ++ extraMessages
      )
      lastMessage <-
        bus.subscribe.collect[Event] {
          case Message(_, `roomId`, e: Event.GameCompleted) => e
          case Message(_, `roomId`, Event.GameAborted) => Event.GameAborted
        }
        .concurrently(bus.run)
        .concurrently(if (realSpeed) GameService.run(bus, gameRepo, messageRepo) else GameService.run(bus, gameRepo, messageRepo, _ => 2.millis))
        .concurrently(Lobby.run(bus, roomRepo))
        .concurrently(activateStream)
        .concurrently(playStreams)
        .take(1)
        .interruptAfter(1.minute)
        .compile
        .lastOrError
    } yield lastMessage).unsafeRunSync()
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
