package bastoni.domain

import bastoni.domain.model.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import io.circe.syntax.*
import io.circe.parser.parse

class JsonSpec extends AnyFreeSpec with Matchers:

  "Encoding of match players" in {
    val player = MatchPlayer(
      GamePlayer(Player(PlayerId.newId, "John"), 15),
      List(Card(Rank.Asso, Suit.Denari), Card(Rank.Sette, Suit.Denari)),
      Nil
    )

    val expectedJson = parse(
      s"""
         |{
         |  "id": "${player.id}",
         |  "name": "John",
         |  "points": 15,
         |  "hand": [
         |    ["Asso", "Denari"],
         |    ["Sette", "Denari"]
         |  ],
         |  "taken": []
         |}""".stripMargin
    ).getOrElse(fail("Invalid json"))

    player.asJson shouldBe expectedJson
    expectedJson.as[MatchPlayer] shouldBe Right(player)
  }

  "Encoding of game players" in {
    val gamePlayer = GamePlayer(Player(PlayerId.newId, "John"), 15)

    val expectedJson = parse(
      s"""
         |{
         |  "id": "${gamePlayer.id}",
         |  "name": "John",
         |  "points": 15
         |}""".stripMargin
    ).getOrElse(fail("Invalid json"))

    gamePlayer.asJson shouldBe expectedJson
    expectedJson.as[GamePlayer] shouldBe Right(gamePlayer)
  }

  "Server events are encoded / decoded with a discriminant property" - {
    "case class" in {
      val expectedJson = parse("""{"type": "DeckShuffled", "cards": [["Asso","Denari"],["Due","Coppe"]]}""").getOrElse(fail("Invalid json"))
      val event: ServerEvent = Event.DeckShuffled(List(Card(Rank.Asso, Suit.Denari), Card(Rank.Due, Suit.Coppe)))

      event.asJson shouldBe expectedJson
      expectedJson.as[ServerEvent] shouldBe Right(event)
    }

    "case object" in {
      val expectedJson = parse("""{"type": "MatchAborted"}""").getOrElse(fail("Invalid json"))
      val event: ServerEvent = Event.MatchAborted

      event.asJson shouldBe expectedJson
      expectedJson.as[ServerEvent] shouldBe Right(event)
    }
  }

  "Commands are encoded / decoded with a discriminant property" - {
    "case class" in {
      val expectedJson = parse("""{"type": "ShuffleDeck", "seed": 15}""").getOrElse(fail("Invalid json"))
      val command: Command = Command.ShuffleDeck(15)

      command.asJson shouldBe expectedJson
      expectedJson.as[Command] shouldBe Right(command)
    }

    "case object" in {
      val expectedJson = parse("""{"type": "Continue"}""").getOrElse(fail("Invalid json"))
      val command: Command = Command.Continue

      command.asJson shouldBe expectedJson
      expectedJson.as[Command] shouldBe Right(command)
    }
  }

  "Messages are encoded / decoded with a discriminant property" - {
    "command" in {
      val messageId = MessageId.newId
      val roomId = RoomId.newId

      val expectedJson = parse(s"""{"id": "$messageId", "roomId": "$roomId", "type": "Command", "data": {"type": "Continue"}}""").getOrElse(fail("Invalid json"))
      val message = Message(messageId, roomId, Command.Continue)

      message.asJson shouldBe expectedJson
      expectedJson.as[Message] shouldBe Right(message)
    }
  }
