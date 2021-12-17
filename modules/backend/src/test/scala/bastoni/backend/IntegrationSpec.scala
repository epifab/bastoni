package bastoni.backend

import bastoni.domain.model.*
import bastoni.domain.model.Command.Continue
import bastoni.domain.view.{FromPlayer, ToPlayer}
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.ToPlayer.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

object DumbPlayer:
  def apply(me: Player, roomId: RoomId, gameBus: GameBus[IO]): fs2.Stream[IO, Unit] =
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
          case (ActionRequest(playerId, Command.Action.ShuffleDeck), Some(player)) if player.is(playerId) => ShuffleDeck
        }
    )


class IntegrationSpec extends AnyFreeSpec with Matchers:

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")

  val roomId = RoomId.newId
  val messageBus: IO[MessageBus[IO]] = MessageBus.inMemory[IO]

  "Three players can play an entire game" in {
    val result: Option[Event] = (for {
      bus <- messageBus
      gameBus = GameBus(bus)
      playStream1 = DumbPlayer(player1, roomId, gameBus)
      playStream2 = DumbPlayer(player2, roomId, gameBus)
      playStream3 = DumbPlayer(player3, roomId, gameBus)
      logEvents = bus.subscribe.evalMap { case Message(_, event) => IO(println(event)) }
      activateStream = gameBus.publish(
        player1,
        roomId,
        fs2.Stream(ActivateRoom(GameType.Briscola)).delayBy[IO](500.millis)
      )
      lastMessage <-
        bus.subscribe.collect[Event] {
          case Message(`roomId`, e: Event.GameCompleted) => e
          case Message(`roomId`, Event.GameAborted) => Event.GameAborted
        }
        .take(1)
        .concurrently(bus.run)
        .concurrently(GameService.run(bus, _ => 5.millis))
        .concurrently(Lobby.run(bus))
        .concurrently(activateStream)
        .concurrently(playStream1)
        .concurrently(playStream2)
        .concurrently(playStream3)
        .concurrently(logEvents)
        .interruptAfter(1.minute)
        .compile
        .last
    } yield lastMessage).unsafeRunSync()

    result shouldBe a[Some[Event.GameCompleted]]
  }
