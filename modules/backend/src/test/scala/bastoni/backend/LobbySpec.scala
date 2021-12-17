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

  private val lobby: fs2.Stream[fs2.Pure, Message] => fs2.Stream[fs2.Pure, MessageOut] =
    Lobby[fs2.Pure](2)

  "Rooms can be joined and left" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, JoinRoom(player2)),
      Message(roomId1, LeaveRoom(player1)),
      Message(roomId1, LeaveRoom(player2)),
    )

    lobby(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
      Message(roomId1, PlayerJoined(player2, Room(roomId1, List(player2, player1)))),
      Message(roomId1, PlayerLeft(player1, Room(roomId1, List(player2)))),
      Message(roomId1, PlayerLeft(player2, Room(roomId1, Nil)))
    )
  }

  "Players cannot join a room that is full" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, JoinRoom(player2)),
      Message(roomId1, JoinRoom(player3))
    )

    lobby(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
      Message(roomId1, PlayerJoined(player2, Room(roomId1, List(player2, player1)))),
    )
  }

  "Players can join multiple rooms" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, JoinRoom(player2)),
      Message(roomId2, JoinRoom(player1)),
    )

    lobby(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
      Message(roomId1, PlayerJoined(player2, Room(roomId1, List(player2, player1)))),
      Message(roomId2, PlayerJoined(player1, Room(roomId2, List(player1))))
    )
  }

  "Messages from different rooms won't interfere" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId2, LeaveRoom(player1)),  // will be ignored as room2 doesn't exist
    )

    lobby(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
    )
  }

  "Players cannot join the same room twice" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, JoinRoom(player1)), // will be ignored
    )

    lobby(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
    )
  }

  "Players cannot leave the same room twice" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, LeaveRoom(player1)),
      Message(roomId1, LeaveRoom(player1)), // will be ignored
    )

    lobby(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
      Message(roomId1, PlayerLeft(player1, Room(roomId1, Nil))),
    )
  }

  "Random messages will be ignored" in {
    val commands = fs2.Stream(
      Message(roomId1, JoinRoom(player1)),
      Message(roomId1, PlayCard(player1.id, Card(Rank.Sette, Suit.Denari))) // will be ignored
    )

    lobby(commands).compile.toList shouldBe List(
      Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1))))
    )
  }

  "Activation" - {
    "A room with 1 player cannot be activated" in {
      val commands = fs2.Stream(
        Message(roomId1, JoinRoom(player1)),
        Message(roomId1, ActivateRoom(player1, GameType.Briscola))
      )

      lobby(commands).compile.toList shouldBe List(
        Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
      )
    }

    "A room with 2 player can be activated" in {
      val commands = fs2.Stream(
        Message(roomId1, JoinRoom(player1)),
        Message(roomId1, JoinRoom(player2)),
        Message(roomId1, ActivateRoom(player1, GameType.Briscola))
      )

      lobby(commands).compile.toList shouldBe List(
        Message(roomId1, PlayerJoined(player1, Room(roomId1, List(player1)))),
        Message(roomId1, PlayerJoined(player2, Room(roomId1, List(player2, player1)))),
        Message(roomId1, StartGame(Room(roomId1, List(player2, player1)), GameType.Briscola)),
      )
    }

  }
