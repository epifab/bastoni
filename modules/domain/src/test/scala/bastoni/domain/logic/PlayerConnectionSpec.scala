package bastoni.domain.logic

import bastoni.domain.AsyncIOFreeSpec
import bastoni.domain.model.Event.{PlayerJoinedTable, PlayerLeftTable}
import bastoni.domain.model.{RoomId, User, UserId}
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.IO

class PlayerConnectionSpec extends AsyncIOFreeSpec:
  private val me     = User(UserId.newId, "me")
  private val roomId = RoomId.newId

  "A player can connect and join a table" in {
    val input = fs2.Stream(
      FromPlayer.Connect,
      FromPlayer.JoinTable,
      FromPlayer.LeaveTable
    )

    Services
      .inMemory[IO]
      .use { case (controller, run) =>
        controller
          .connectPlayer(me, roomId)
          .take(3)
          .concurrently(controller.publish(me, roomId)(input))
          .concurrently(run)
          .compile
          .toList
      }
      .asserting {
        case (msg1, room1) :: (msg2, room2) :: (msg3, room3) :: Nil =>
          msg1 shouldBe a[ToPlayer.Connected]
          msg2 match
            case ToPlayer.GameEvent(PlayerJoinedTable(joiner, _)) =>
              joiner shouldBe me
            case _ =>
              fail("Unexpected message #2")
          msg3 match
            case ToPlayer.GameEvent(PlayerLeftTable(leaver, _)) =>
              leaver shouldBe me
            case _ =>
              fail("Unexpected message #3")

          room1.fold(fail("Room not returned"))(_.players.get(me.id) shouldBe None)
          room2.fold(fail("Room not returned"))(_.players.get(me.id) shouldBe Some(me))
          room2.fold(fail("Room not returned"))(_.seatFor(me) shouldBe defined)
          room3.fold(fail("Room not returned"))(_.players.get(me.id) shouldBe Some(me))
          room3.fold(fail("Room not returned"))(_.seatFor(me) should not be defined)

        case msgs => fail(s"Unexpected number of messages produced: ${msgs.length}")
      }
  }
end PlayerConnectionSpec
