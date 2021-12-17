package bastoni.backend
package briscola

import bastoni.backend.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class BriscolaGameServiceSpec extends AnyFreeSpec with Matchers:

  val room1 = Room(RoomId.newId, List(player1, player2))
  val room2 = Room(RoomId.newId, List(player2, player3))

  "Two simultaneous briscola matches can be played" in {
    val input = fs2.Stream(
      StartGame(room1, GameType.Briscola).toMessage(room1.id),
      StartGame(room2, GameType.Briscola).toMessage(room2.id),
      ShuffleDeck(10).toMessage(room1.id),
      Continue.toMessage(room1.id),
      ShuffleDeck(10).toMessage(room2.id),
      Continue.toMessage(room1.id),
      Continue.toMessage(room2.id),
      PlayerLeft(player1, Room(room1.id, List(player2))).toMessage(room1.id),
    )

    GameService(messageIds)(input).compile.toList shouldBe List(
      GameStarted(GameType.Briscola).toMessage(room1.id),
      GameStarted(GameType.Briscola).toMessage(room2.id),
      DeckShuffled(10).toMessage(room1.id),
      Delayed(Continue.toMessage(room1.id), Delay.Medium),
      CardDealt(player1.id, Card(Due, Bastoni)).toMessage(room1.id),
      Delayed(Continue.toMessage(room1.id), Delay.Short),
      DeckShuffled(10).toMessage(room2.id),
      Delayed(Continue.toMessage(room2.id), Delay.Medium),
      CardDealt(player2.id, Card(Asso,Spade)).toMessage(room1.id),
      Delayed(Continue.toMessage(room1.id), Delay.Short),
      CardDealt(player2.id, Card(Due, Bastoni)).toMessage(room2.id),
      Delayed(Continue.toMessage(room2.id), Delay.Short),
      MatchAborted.toMessage(room1.id),
      GameAborted.toMessage(room1.id),
    )
  }

  "A complete game can be played" in {
    val room = Room(RoomId.newId, List(player1, player2, player3))

    val inputStream =
      fs2.Stream(
        StartGame(room, GameType.Briscola).toMessage(room.id),
        GameStarted(GameType.Briscola).toMessage(room.id)
      ) ++
      Briscola3Spec.input(room.id, player1, player2, player3) ++
      Briscola3Spec.input(room.id, player2, player3, player1) ++
      Briscola3Spec.input(room.id, player3, player1, player2) ++
      Briscola3Spec.input(room.id, player1, player2, player3) ++
      fs2.Stream(Continue.toMessage(room.id))

    val outputStream =
      GameStarted(GameType.Briscola).toMessage(room.id) ::
      (ActionRequest(player3.id, Action.ShuffleDeck).toMessage(room.id) ::
      Briscola3Spec.output(room.id, player1, player2, player3)) ++
      (ActionRequest(player1.id, Action.ShuffleDeck).toMessage(room.id) ::
      Briscola3Spec.output(room.id, player2, player3, player1)) ++
      (ActionRequest(player2.id, Action.ShuffleDeck).toMessage(room.id) ::
      Briscola3Spec.output(room.id, player3, player1, player2)) ++
      (ActionRequest(player3.id, Action.ShuffleDeck).toMessage(room.id) ::
      Briscola3Spec.output(room.id, player1, player2, player3)) ++
      List(GameCompleted(List(player1.id)).toMessage(room.id))

    GameService(messageIds)(inputStream).compile.toList shouldBe outputStream
  }
