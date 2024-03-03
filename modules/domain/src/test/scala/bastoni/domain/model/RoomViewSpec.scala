package bastoni.domain.model

import bastoni.domain.model.Event.PlayerJoinedTable
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class RoomViewSpec extends AnyFreeSpec with Matchers:

  val me: User = User(UserId.newId, "me")
  val emptyRoom: RoomPlayerView = RoomPlayerView(
    me.id,
    List(
      EmptySeat(1, Nil, Nil),
      EmptySeat(2, Nil, Nil)
    ),
    Nil,
    Nil,
    None,
    None,
    Map.empty
  )

  "Player joining an empty table" in {
    val room: RoomPlayerView = emptyRoom.update(PlayerJoinedTable(me, 1))
    room.players.get(me.id) shouldBe Some(me)
  }
