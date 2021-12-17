package bastoni.backend

import bastoni.domain.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class LobbySpec extends AnyFreeSpec with Matchers:

  val roomId = RoomId.newId

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")

  "Rooms can be joined and left" in {
    val commands = fs2.Stream(
      JoinRoom(player1),
      JoinRoom(player2),
      JoinRoom(player2),  // will be ignored (player already in there)
      JoinRoom(player3),  // will be ignored (room was full)
      LeaveRoom(player1),
      JoinRoom(player3)
    ).map(Message(roomId, _))

    Lobby[fs2.Pure](roomId, 2, commands).map(_.message).compile.toList shouldBe List(
      PlayerJoined(player1, Room(roomId, List(player1))),
      PlayerJoined(player2, Room(roomId, List(player2, player1))),
      PlayerLeft(player1, Room(roomId, List(player2))),
      PlayerJoined(player3, Room(roomId, List(player3, player2)))
    )
  }

  "Messages from different rooms won't interfere" in {
    val commands = fs2.Stream(
      Message(roomId, JoinRoom(player1)),
      Message(RoomId.newId, LeaveRoom(player1))
    )

    Lobby[fs2.Pure](roomId, 2, commands).map(_.message).compile.toList shouldBe List(
      PlayerJoined(player1, Room(roomId, List(player1))),
    )
  }
