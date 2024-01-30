package bastoni.domain

import bastoni.domain.model.*
import io.circe.parser.parse
import io.circe.syntax.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class JsonSpec extends AnyFreeSpec with Matchers:

  "Encoding of players" in {
    val player = Player(
      MatchPlayer(User(UserId.newId, "John"), 15),
      List(
        VisibleCard(Rank.Asso, Suit.Denari, CardId(5)),
        VisibleCard(Rank.Sette, Suit.Denari, CardId(7))
      ),
      Nil
    )

    val expectedJson = parse(
      s"""
         |{
         |  "id": "${player.id}",
         |  "name": "John",
         |  "points": 15,
         |  "hand": [
         |    {"rank": "Asso", "suit": "Denari", "ref": 5},
         |    {"rank": "Sette", "suit": "Denari", "ref": 7}
         |  ],
         |  "pile": [],
         |  "extraPoints": 0
         |}""".stripMargin
    ).getOrElse(fail("Invalid json"))

    player.asJson shouldBe expectedJson
    expectedJson.as[Player] shouldBe Right(player)
  }

  "Encoding of match players" in {
    val player = MatchPlayer(User(UserId.newId, "John"), 15)

    val expectedJson = parse(
      s"""
         |{
         |  "id": "${player.id}",
         |  "name": "John",
         |  "points": 15
         |}""".stripMargin
    ).getOrElse(fail("Invalid json"))

    player.asJson shouldBe expectedJson
    expectedJson.as[MatchPlayer] shouldBe Right(player)
  }

  "Server events are encoded / decoded with a discriminant property" - {
    "case class" in {
      val expectedJson =
        parse(
          """{"eventType": "DeckShuffled", "deck": [{"rank": "Asso", "suit": "Denari", "ref": 0},{"rank": "Due", "suit": "Coppe", "ref": 1}]}"""
        ).getOrElse(fail("Invalid json"))
      val event: ServerEvent =
        Event.DeckShuffled(Deck(Card(Rank.Asso, Suit.Denari, CardId(0)), Card(Rank.Due, Suit.Coppe, CardId(1))))

      event.asJson shouldBe expectedJson
      expectedJson.as[ServerEvent] shouldBe Right(event)
    }
  }

  "Commands are encoded / decoded with a discriminant property" - {
    "case class" in {
      val expectedJson     = parse("""{"type": "ShuffleDeck", "seed": 15}""").getOrElse(fail("Invalid json"))
      val command: Command = Command.ShuffleDeck(15)

      command.asJson shouldBe expectedJson
      expectedJson.as[Command] shouldBe Right(command)
    }

    "case object" in {
      val expectedJson     = parse("""{"type": "Continue"}""").getOrElse(fail("Invalid json"))
      val command: Command = Command.Continue

      command.asJson shouldBe expectedJson
      expectedJson.as[Command] shouldBe Right(command)
    }
  }

  "Messages are encoded / decoded with a discriminant property" - {
    "command" in {
      val messageId = MessageId.newId
      val roomId    = RoomId.newId

      val expectedJson = parse(
        s"""{"id": "$messageId", "roomId": "$roomId", "type": "Command", "data": {"type": "Continue"}}"""
      ).getOrElse(fail("Invalid json"))
      val message = Message(messageId, roomId, Command.Continue)

      message.asJson shouldBe expectedJson
      expectedJson.as[Message] shouldBe Right(message)
    }
  }
end JsonSpec
