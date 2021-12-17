package bastoni.backend
package briscola

import bastoni.backend.Fixtures.*
import bastoni.backend.GameBus
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import bastoni.domain.view.FromPlayer
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

class BriscolaGameServiceSpec extends AnyFreeSpec with Matchers:

  val room1 = Room(RoomId.newId, List(player1, player2))
  val room2 = Room(RoomId.newId, List(player2, player3))

//  "Two simultaneous briscola matches can be played" in {
//    val input = fs2.Stream(
//      StartGame(room1, GameType.Briscola).toMessage(room1.id),
//      StartGame(room2, GameType.Briscola).toMessage(room2.id),
//      ShuffleDeck(10).toMessage(room1.id),
//      Continue.toMessage(room1.id),
//      ShuffleDeck(10).toMessage(room2.id),
//      Continue.toMessage(room1.id),
//      Continue.toMessage(room2.id),
//      PlayerLeft(player1, Room(room1.id, List(player2))).toMessage(room1.id),
//    )
//
//    GameService[IO](messageIds)(input).compile.toList.unsafeRunSync() shouldBe List(
//      GameStarted(GameType.Briscola).toMessage(room1.id),
//      GameStarted(GameType.Briscola).toMessage(room2.id),
//      DeckShuffled(10).toMessage(room1.id),
//      Delayed(Continue.toMessage(room1.id), Delay.Medium),
//      CardDealt(player1.id, Card(Due, Bastoni)).toMessage(room1.id),
//      Delayed(Continue.toMessage(room1.id), Delay.Short),
//      DeckShuffled(10).toMessage(room2.id),
//      Delayed(Continue.toMessage(room2.id), Delay.Medium),
//      CardDealt(player2.id, Card(Asso,Spade)).toMessage(room1.id),
//      Delayed(Continue.toMessage(room1.id), Delay.Short),
//      CardDealt(player2.id, Card(Due, Bastoni)).toMessage(room2.id),
//      Delayed(Continue.toMessage(room2.id), Delay.Short),
//      MatchAborted.toMessage(room1.id),
//      GameAborted.toMessage(room1.id),
//    )
//  }
//
//  "A complete game can be played" in {
//    val room = Room(RoomId.newId, List(player1, player2, player3))
//
//    val inputStream =
//      fs2.Stream(
//        StartGame(room, GameType.Briscola).toMessage(room.id),
//        GameStarted(GameType.Briscola).toMessage(room.id)
//      ) ++
//      Briscola3Spec.input(room.id, player1, player2, player3) ++
//      Briscola3Spec.input(room.id, player2, player3, player1) ++
//      Briscola3Spec.input(room.id, player3, player1, player2) ++
//      Briscola3Spec.input(room.id, player1, player2, player3) ++
//      fs2.Stream(Continue.toMessage(room.id))
//
//    val outputStream =
//      GameStarted(GameType.Briscola).toMessage(room.id) ::
//      (ActionRequest(player3.id, Action.ShuffleDeck).toMessage(room.id) ::
//      Briscola3Spec.output(room.id, player1, player2, player3)) ++
//      (ActionRequest(player1.id, Action.ShuffleDeck).toMessage(room.id) ::
//      Briscola3Spec.output(room.id, player2, player3, player1)) ++
//      (ActionRequest(player2.id, Action.ShuffleDeck).toMessage(room.id) ::
//      Briscola3Spec.output(room.id, player3, player1, player2)) ++
//      (ActionRequest(player3.id, Action.ShuffleDeck).toMessage(room.id) ::
//      Briscola3Spec.output(room.id, player1, player2, player3)) ++
//      List(GameCompleted(List(player1.id)).toMessage(room.id))
//
//    GameService[IO](messageIds)(inputStream).compile.toList.unsafeRunSync() shouldBe outputStream
//  }

  "A pre-existing game can be resumed" in {
    val gamePlayer1 = GamePlayer(player1, 2)
    val gamePlayer2 = GamePlayer(player2, 1)

    val player1Card = Card(Rank.Tre, Suit.Denari)
    val player2Card = Card(Rank.Asso, Suit.Denari)

    val stateMachine = new briscola.StateMachine(
      briscola.GameState.InProgress(
        List(gamePlayer1, gamePlayer2),
        briscola.MatchState.PlayRound(
          List(MatchPlayer(gamePlayer1, Set(player1Card), Deck.instance.filter(card => card != player1Card && card != player2Card).toSet)),
          List(MatchPlayer(gamePlayer2, Set.empty, Set.empty) -> player2Card),
          Nil,
          player1Card
        ),
        rounds = 0
      )
    )

    val oldMessage = CardPlayed(player2.id, player2Card).toMessage(room1.id)

    val (events, gameRooms, messages) = (for {
      repo <- InMemoryJsonGameServiceRepo[IO]
      _ <- repo.set(room1.id, stateMachine)
      _ <- repo.flying(oldMessage)
      bus <- MessageBus.inMemory[IO]
      events <- (for {
        subscription <- fs2.Stream.resource(bus.subscribeAwait)
        event <- subscription.concurrently(bus.run)
          .concurrently(GameService.run[IO](bus, repo, _ => 2.millis))
          .concurrently(GameBus(bus).publish(player1, room1.id, fs2.Stream(FromPlayer.PlayCard(player1Card)).delayBy(100.millis)))
          .collect { case Message(_, _, event: Event) => event }
          .takeThrough(!_.isInstanceOf[Event.GameCompleted])
          .interruptAfter(1.second)
      } yield event).compile.toList
      gameRoom <- repo.get(room1.id)
      messages <- repo.inFlight.compile.toList
    } yield (events, gameRoom, messages)).unsafeRunSync()

    events shouldBe List(
      oldMessage.data,
      CardPlayed(player1.id, player1Card),
      TrickCompleted(player2.id),
      PointsCount(List(player2.id), 21),
      PointsCount(List(player1.id), 99),
      MatchCompleted(List(player1.id)),
      GameCompleted(List(player1.id))
    )

    gameRooms shouldBe None  // when a game completes, the state machine goes away
    messages shouldBe Nil    // all outstanding events are expected to have been fully processed
  }

  "Future undelivered events will still be sent" in {

    val message1: Delayed[Message] = Delayed(Message(MessageId.newId, room1.id, CardPlayed(player2.id, Card(Rank.Asso, Suit.Coppe))), Delay.Long)
    val message2: Delayed[Message] = Delayed(Message(MessageId.newId, room1.id, CardPlayed(player2.id, Card(Rank.Due, Suit.Coppe))), Delay.Medium)
    val message3: Delayed[Message] = Delayed(Message(MessageId.newId, room1.id, CardPlayed(player2.id, Card(Rank.Tre, Suit.Coppe))), Delay.Short)

    val events = (for {
      bus <- MessageBus.inMemory[IO]
      repo <- InMemoryJsonGameServiceRepo[IO]
      _ <- repo.flying(message1)
      _ <- repo.flying(message2)
      _ <- repo.flying(message3)
      events <- (for {
        subscription <- fs2.Stream.resource(bus.subscribeAwait)
        message <- subscription
          .concurrently(bus.run)
          .concurrently(GameService.run[IO](bus, repo, {
            case Delay.Short => 10.milli
            case Delay.Medium => 20.millis
            case Delay.Long => 30.millis
          }))
          .interruptAfter(100.millis)
      } yield message).compile.toList
    } yield events).unsafeRunSync()

    events shouldBe List(message3.inner, message2.inner, message1.inner)
  }
