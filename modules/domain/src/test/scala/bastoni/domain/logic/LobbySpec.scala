package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import cats.catsInstancesForId
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class LobbySpec extends AnyFreeSpec with Matchers:

  val messageId = MessageId.newId

  val roomId1 = RoomId.newId
  val roomId2 = RoomId.newId

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")

  private val lobby: fs2.Stream[cats.Id, Message] => fs2.Stream[cats.Id, Message] =
    Lobby[cats.Id](2, messageId, 123)

  "Rooms can be joined and left" in {
    val commands = fs2.Stream(
      Message(messageId, roomId1, JoinRoom(player1)),
      Message(messageId, roomId1, JoinRoom(player2)),
      Message(messageId, roomId1, LeaveRoom(player1)),
      Message(messageId, roomId1, LeaveRoom(player2)),
    )

    lobby(commands).compile.toList shouldBe List(
      Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
      Message(messageId, roomId1, PlayerJoined(player2, Room(roomId1, List(Some(player1), Some(player2))))),
      Message(messageId, roomId1, PlayerLeft(player1, Room(roomId1, List(None, Some(player2))))),
      Message(messageId, roomId1, PlayerLeft(player2, Room(roomId1, List(None, None))))
    )
  }

  "Players cannot join a room that is full" in {
    val commands = fs2.Stream(
      Message(messageId, roomId1, JoinRoom(player1)),
      Message(messageId, roomId1, JoinRoom(player2)),
      Message(messageId, roomId1, JoinRoom(player3))
    )

    lobby(commands).compile.toList shouldBe List(
      Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
      Message(messageId, roomId1, PlayerJoined(player2, Room(roomId1, List(Some(player1), Some(player2))))),
    )
  }

  "Players can join multiple rooms" in {
    val commands = fs2.Stream(
      Message(messageId, roomId1, JoinRoom(player1)),
      Message(messageId, roomId1, JoinRoom(player2)),
      Message(messageId, roomId2, JoinRoom(player1)),
    )

    lobby(commands).compile.toList shouldBe List(
      Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
      Message(messageId, roomId1, PlayerJoined(player2, Room(roomId1, List(Some(player1), Some(player2))))),
      Message(messageId, roomId2, PlayerJoined(player1, Room(roomId2, List(Some(player1), None))))
    )
  }

  "Messages from different rooms won't interfere" in {
    val commands = fs2.Stream(
      Message(messageId, roomId1, JoinRoom(player1)),
      Message(messageId, roomId2, LeaveRoom(player1)),  // will be ignored as room2 doesn't exist
    )

    lobby(commands).compile.toList shouldBe List(
      Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
    )
  }

  "Players cannot join the same room twice" in {
    val commands = fs2.Stream(
      Message(messageId, roomId1, JoinRoom(player1)),
      Message(messageId, roomId1, JoinRoom(player1)), // will be ignored
    )

    lobby(commands).compile.toList shouldBe List(
      Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
    )
  }

  "Players cannot leave the same room twice" in {
    val commands = fs2.Stream(
      Message(messageId, roomId1, JoinRoom(player1)),
      Message(messageId, roomId1, LeaveRoom(player1)),
      Message(messageId, roomId1, LeaveRoom(player1)), // will be ignored
    )

    lobby(commands).compile.toList shouldBe List(
      Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
      Message(messageId, roomId1, PlayerLeft(player1, Room(roomId1, List(None, None)))),
    )
  }

  "Random messages will be ignored" in {
    val commands = fs2.Stream(
      Message(messageId, roomId1, JoinRoom(player1)),
      Message(messageId, roomId1, PlayCard(player1.id, Card(Rank.Sette, Suit.Denari))) // will be ignored
    )

    lobby(commands).compile.toList shouldBe List(
      Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None))))
    )
  }

  "Activation" - {
    "A room with 1 player cannot be activated" in {
      val commands = fs2.Stream(
        Message(messageId, roomId1, JoinRoom(player1)),
        Message(messageId, roomId1, ActivateRoom(player1, GameType.Briscola))
      )

      lobby(commands).compile.toList shouldBe List(
        Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
      )
    }

    "A room with 2 player can be activated" in {
      val commands = fs2.Stream(
        Message(messageId, roomId1, JoinRoom(player1)),
        Message(messageId, roomId1, JoinRoom(player2)),
        Message(messageId, roomId1, ActivateRoom(player1, GameType.Briscola))
      )

      lobby(commands).compile.toList shouldBe List(
        Message(messageId, roomId1, PlayerJoined(player1, Room(roomId1, List(Some(player1), None)))),
        Message(messageId, roomId1, PlayerJoined(player2, Room(roomId1, List(Some(player1), Some(player2))))),
        Message(messageId, roomId1, StartGame(Room(roomId1, List(Some(player1), Some(player2))), GameType.Briscola)),
      )
    }

  }
