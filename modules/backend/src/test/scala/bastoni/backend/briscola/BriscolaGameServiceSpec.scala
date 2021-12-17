package bastoni.backend
package briscola

import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class BriscolaGameServiceSpec extends AnyFreeSpec with Matchers:

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")

  val room1 = Room(RoomId.newId, List(player1, player2))
  val room2 = Room(RoomId.newId, List(player2, player3))

  "Two simultaneous briscola matches can be played" in {
    val input = fs2.Stream(
      Message(room1.id, StartGame(room1, GameType.Briscola)),
      Message(room2.id, StartGame(room2, GameType.Briscola)),
      Message(room1.id, ShuffleDeck(10)),
      Message(room1.id, Continue),
      Message(room2.id, ShuffleDeck(10)),
      Message(room1.id, Continue),
      Message(room2.id, Continue),
      Message(room1.id, PlayerLeft(player1, Room(room1.id, List(player2)))),
    )

    GameService(input).compile.toList shouldBe List(
      Message(room1.id, GameStarted(GameType.Briscola)),
      Message(room2.id, GameStarted(GameType.Briscola)),
      Message(room1.id, DeckShuffled(10)),
      DelayedMessage(Message(room1.id, Continue), Delay.Medium),
      Message(room1.id, CardDealt(player1.id, Card(Due, Bastoni))),
      DelayedMessage(Message(room1.id, Continue), Delay.Short),
      Message(room2.id, DeckShuffled(10)),
      DelayedMessage(Message(room2.id, Continue), Delay.Medium),
      Message(room1.id, CardDealt(player2.id, Card(Asso,Spade))),
      DelayedMessage(Message(room1.id, Continue), Delay.Short),
      Message(room2.id, CardDealt(player2.id, Card(Due, Bastoni))),
      DelayedMessage(Message(room2.id, Continue), Delay.Short),
      Message(room1.id, MatchAborted),
      Message(room1.id, GameAborted),
    )
  }

  "A complete game can be played" in {
    val room = Room(RoomId.newId, List(player1, player2, player3))

    val inputStream =
      fs2.Stream(
        Message(room.id, StartGame(room, GameType.Briscola)),
        Message(room.id, GameStarted(GameType.Briscola))
      ) ++
      Briscola3Spec.input(room.id, player1, player2, player3) ++
      Briscola3Spec.input(room.id, player2, player3, player1) ++
      Briscola3Spec.input(room.id, player3, player1, player2) ++
      Briscola3Spec.input(room.id, player1, player2, player3) ++
      fs2.Stream(Message(room.id, Continue))

    val outputStream =
      Message(room.id, GameStarted(GameType.Briscola)) ::
      (Message(room.id, ActionRequest(player3.id, Action.ShuffleDeck)) ::
      Briscola3Spec.output(room.id, player1, player2, player3)) ++
      (Message(room.id, ActionRequest(player1.id, Action.ShuffleDeck)) ::
      Briscola3Spec.output(room.id, player2, player3, player1)) ++
      (Message(room.id, ActionRequest(player2.id, Action.ShuffleDeck)) ::
      Briscola3Spec.output(room.id, player3, player1, player2)) ++
      (Message(room.id, ActionRequest(player3.id, Action.ShuffleDeck)) ::
      Briscola3Spec.output(room.id, player1, player2, player3)) ++
      List(Message(room.id, GameCompleted(List(player1.id))))

    GameService(inputStream).compile.toList shouldBe outputStream
  }
