package bastoni.backend

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.ToPlayer.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class IntegrationSpec extends AnyFreeSpec with Matchers:

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val roomId = RoomId.newId
  val messageBus: IO[MessageBus[IO]] = MessageBus.inMemory[IO]

  "Two players can play" in {
    val result = for {
      bus <- messageBus
      gameBus = GameBus(bus, fs2.Stream.constant(10))
      message <- gameBus
        .subscribe(player1, roomId)
        .take(6)
        .interruptAfter(5.seconds)
        .concurrently(bus.run)
        .concurrently(Lobby.run(bus))
        .concurrently(GameService.run(bus))
        .concurrently(gameBus.publish(
          player1,
          roomId,
          fs2.Stream(JoinRoom)).delayBy(50.millis)
        )
        .concurrently(gameBus.publish(
          player2,
          roomId,
          fs2.Stream(JoinRoom, ActivateRoom(GameType.Briscola)) ++ fs2.Stream(ShuffleDeck).delayBy(50.millis)
        ).delayBy(100.millis))
        .compile
        .toList
    } yield message

    result.unsafeRunSync() shouldBe List(
      PlayerJoined(player1, Room(roomId, List(player1))),
      PlayerJoined(player2, Room(roomId, List(player2, player1))),
      DeckShuffled,
      CardDealt(player2.id, None),
      CardDealt(player1.id, Some(Card(Rank.Asso, Suit.Spade))),
      CardDealt(player2.id, None),
    )
  }
