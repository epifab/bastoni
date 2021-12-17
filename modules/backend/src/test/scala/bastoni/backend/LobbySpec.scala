package bastoni.backend

import bastoni.domain.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class LobbySpec extends AnyFreeSpec with Matchers:

  val roomId1 = RoomId.newId
  val roomId2 = RoomId.newId

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")

  "Rooms can be joined and left" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, JoinRoom(player2)),
      Message(roomId1, JoinRoom(player2)),  // will be ignored (player already in there)
      Message(roomId1, JoinRoom(player3)),  // will be ignored (room was full)
      Message(roomId1, LeaveRoom(player1)),
      Message(roomId1, JoinRoom(player3))
    )

    Lobby[fs2.Pure](2)(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
      Message(roomId1, PlayerJoined(player2, Room(roomId1, List(player2, player1)))),
      Message(roomId1, PlayerLeft(player1, Room(roomId1, List(player2)))),
      Message(roomId1, PlayerJoined(player3, Room(roomId1, List(player3, player2))))
    )
  }

  "Messages from different rooms won't interfere" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, JoinRoom(player2)),
      Message(roomId2, LeaveRoom(player1)),  // will be ignored
      Message(roomId2, JoinRoom(player2)),
      Message(roomId2, LeaveRoom(player2)),
    )

    Lobby[fs2.Pure](2)(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
      Message(roomId1, PlayerJoined(player2, Room(roomId1, List(player2, player1)))),
      Message(roomId2, PlayerJoined(player2, Room(roomId2, List(player2)))),
      Message(roomId2, PlayerLeft(player2, Room(roomId2, Nil))),
    )
  }
