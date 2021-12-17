package bastoni.domain.logic

import bastoni.domain.AsyncIOFreeSpec
import bastoni.domain.logic.Fixtures.*
import bastoni.domain.logic.briscola.Briscola3Spec
import bastoni.domain.logic.{GameContext, GamePubSub}
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import bastoni.domain.model.*
import bastoni.domain.repos.{GameRepo, MessageRepo}
import bastoni.domain.view.FromPlayer
import cats.effect.IO

import scala.concurrent.duration.DurationInt

class GameServiceSpec extends AsyncIOFreeSpec:

  val room1Id = RoomId.newId
  val room2Id = RoomId.newId
  val room1Players = List(user1, user2)
  val room2Players = List(user2, user3)

  def gameService(inputStream: fs2.Stream[IO, Message]): IO[List[Message | Delayed[Message]]] = for {
    gameRepo <- GameRepo.inMemory[IO]
    messageRepo <- MessageRepo.inMemory[IO]
    output <- GameService[IO](IO.pure(messageId), gameRepo, messageRepo)(inputStream).compile.toList
  } yield output

  "Rooms can be joined and left" in {
    val commands = fs2.Stream(
      JoinTable(user1, joinSeed),
      JoinTable(user2, joinSeed),
      LeaveTable(user1),
      LeaveTable(user2)
    ).map(_.toMessage(room1Id))

    gameService(commands).asserting(_ shouldBe List(
      PlayerJoinedTable(user1, 3),
      PlayerJoinedTable(user2, 2),
      PlayerLeftTable(user1, 3),
      PlayerLeftTable(user2, 2)
    ).map(_.toMessage(room1Id)))
  }

  "Players cannot join a room that is full" in {
    val commands = fs2.Stream(
      JoinTable(user1, joinSeed),
      JoinTable(user2, joinSeed),
      JoinTable(user3, joinSeed),
      JoinTable(user4, joinSeed),
      JoinTable(user5, joinSeed),
    ).map(_.toMessage(room1Id))

    gameService(commands).asserting(_ shouldBe List(
      PlayerJoinedTable(user1, 3),
      PlayerJoinedTable(user2, 2),
      PlayerJoinedTable(user3, 1),
      PlayerJoinedTable(user4, 0)
    ).map(_.toMessage(room1Id)))
  }

  "Players can join multiple rooms" in {
    val commands = fs2.Stream(
      JoinTable(user1, joinSeed).toMessage(room1Id),
      JoinTable(user1, joinSeed).toMessage(room2Id)
    )

    gameService(commands).asserting(_ shouldBe List(
      PlayerJoinedTable(user1, 3).toMessage(room1Id),
      PlayerJoinedTable(user1, 3).toMessage(room2Id)
    ))
  }

  "Messages from different rooms won't interfere" in {
    val commands = fs2.Stream(
      JoinTable(user1, joinSeed).toMessage(room1Id),
      LeaveTable(user1).toMessage(room2Id),  // will be ignored as room2 doesn't exist
    )

    gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(user1, 3).toMessage(room1Id)))
  }

  "Players cannot join the same room twice" in {
    val commands = fs2.Stream(
      JoinTable(user1, joinSeed).toMessage(room1Id),
      JoinTable(user1, joinSeed).toMessage(room1Id),  // will be ignored
    )

    gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(user1, 3).toMessage(room1Id)))
  }

  "Random messages will be ignored" in {
    val commands = fs2.Stream(
      JoinTable(user1, joinSeed).toMessage(room1Id),
      PlayCard(user1.id, Card(Rank.Sette, Suit.Denari)).toMessage(room1Id) // will be ignored
    )

    gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(user1, 3).toMessage(room1Id)))
  }

  "A new game" - {
    "cannot start if only players is at the table" in {
      val commands = fs2.Stream(
        JoinTable(user1, joinSeed),
        StartGame(user1.id, GameType.Briscola)
      ).map(_.toMessage(room1Id))

      gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(user1, 3).toMessage(room1Id)))
    }

    "can start for 2 players" in {
      val commands = fs2.Stream(
        JoinTable(user1, joinSeed),
        JoinTable(user2, joinSeed),
        StartGame(user1.id, GameType.Briscola)
      ).map(_.toMessage(room1Id))

      gameService(commands).asserting(_ shouldBe List(
        PlayerJoinedTable(user1, 3).toMessage(room1Id),
        PlayerJoinedTable(user2, 2).toMessage(room1Id),
        GameStarted(GameType.Briscola).toMessage(room1Id)
      ))
    }

  }


  "Two simultaneous matches can be played" in {
    val inputStream = fs2.Stream(
      JoinTable(user2, joinSeed).toMessage(room1Id),
      JoinTable(user1, joinSeed).toMessage(room1Id),
      JoinTable(user3, joinSeed).toMessage(room2Id),
      JoinTable(user2, joinSeed).toMessage(room2Id),

      StartGame(user2.id, GameType.Briscola).toMessage(room1Id),
      StartGame(user2.id, GameType.Tressette).toMessage(room2Id),

      ShuffleDeck(shuffleSeed).toMessage(room1Id),
      Continue.toMessage(room1Id),
      ShuffleDeck(shuffleSeed).toMessage(room2Id),
      Continue.toMessage(room2Id),
      LeaveTable(user1).toMessage(room1Id),
      PlayerLeftTable(user1, 2).toMessage(room1Id)
    )

    gameService(inputStream).asserting(_ shouldBe List(
      PlayerJoinedTable(user2, 3).toMessage(room1Id),
      PlayerJoinedTable(user1, 2).toMessage(room1Id),
      PlayerJoinedTable(user3, 3).toMessage(room2Id),
      PlayerJoinedTable(user2, 2).toMessage(room2Id),

      GameStarted(GameType.Briscola).toMessage(room1Id),
      GameStarted(GameType.Tressette).toMessage(room2Id),

      DeckShuffled(shuffledDeck).toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.Medium),
      CardsDealt(user1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player).toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.Short),
      DeckShuffled(shuffledDeck).toMessage(room2Id),
      Delayed(Continue.toMessage(room2Id), Delay.Medium),
      CardsDealt(user2.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari), Card(Quattro, Spade), Card(Sei, Denari)), Direction.Player).toMessage(room2Id),
      Delayed(Continue.toMessage(room2Id), Delay.Short),
      PlayerLeftTable(user1, 2).toMessage(room1Id),
      GameAborted.toMessage(room1Id),
      MatchAborted.toMessage(room1Id),
    ))
  }

  "A complete game can be played" ignore {
    val inputStream =
      fs2.Stream[fs2.Pure, ServerEvent | Command](
        JoinTable(user3, joinSeed),
        JoinTable(user2, joinSeed),
        JoinTable(user1, joinSeed),
        StartGame(user1.id, GameType.Briscola),
        GameStarted(GameType.Briscola)
      ).map(_.toMessage(room1Id)) ++
      Briscola3Spec.input(room1Id, user1, user2, user3) ++
      Briscola3Spec.input(room1Id, user2, user3, user1) ++
      Briscola3Spec.input(room1Id, user3, user1, user2) ++
      Briscola3Spec.input(room1Id, user1, user2, user3) ++
      fs2.Stream(Continue.toMessage(room1Id))

    gameService(inputStream).asserting(_.last shouldBe MatchCompleted(List(user1.id)).toMessage(room1Id))
  }

  "A pre-existing game can be resumed and completed" in {
    val player1 = MatchPlayer(user1, 2)
    val player2 = MatchPlayer(user2, 1)

    val player1Card = Card(Rank.Tre, Suit.Denari)
    val player2Card = Card(Rank.Asso, Suit.Denari)

    val player1Collected = Deck.instance.filter(card => card != player1Card && card != player2Card)

    val initialContext = new GameContext(
      table = new TableServerView(
        seats = List(
          Seat(
            player = Some(ActingPlayer(player1, Action.PlayCard, Some(Timeout.Max))),
            hand = List(CardServerView(player1Card, Direction.Player)),
            taken = player1Collected.map(card => CardServerView(card, Direction.Down)),
            played = Nil
          ),
          Seat(
            player = Some(WaitingPlayer(player2)),
            hand = Nil,
            taken = Nil,
            played = List(CardServerView(player2Card, Direction.Up))
          )
        ),
        deck = Nil,
        board = Nil,
        active = true
      ),
      stateMachine = Some(new briscola.StateMachine(
        briscola.MatchState.InProgress(
          List(player1, player2),
          briscola.GameState.PlayRound(
            List(Player(player1, List(player1Card), player1Collected)),
            List(Player(player2, Nil, Nil) -> player2Card),
            Nil,
            player1Card
          ),
          rounds = 0
        )
      ))
    )

    val oldMessage = CardPlayed(user2.id, player2Card).toMessage(room1Id)

    val resultIO = (for {
      gameRepo <- JsonRepos.gameRepo
      messageRepo <- JsonRepos.messageRepo
      _ <- gameRepo.set(room1Id, initialContext)
      _ <- messageRepo.flying(oldMessage)
      messageBus <- MessageBus.inMemory[IO]
      events <- (for {
        subscription <- fs2.Stream.resource(messageBus.subscribeAwait)
        gameServiceRunner <- fs2.Stream.resource(GameService.runner[IO](messageBus, gameRepo, messageRepo, _ => 2.millis))
        event <- subscription
          .concurrently(messageBus.run)
          .concurrently(gameServiceRunner)
          .concurrently(
            fs2.Stream(FromPlayer.PlayCard(player1Card)).delayBy[IO](100.millis)
              .through(GamePubSub.publisher(messageBus).publish(user1, room1Id))
          )
          .collect { case Message(_, _, event: Event) => event }
          .interruptAfter(2.seconds)
      } yield event).compile.toList
      newContext <- gameRepo.get(room1Id)
      messages <- messageRepo.inFlight.compile.toList
    } yield (events, newContext, messages))

    resultIO.asserting { case (events, newContext, messages) =>
      events shouldBe List(
        oldMessage.data,
        CardPlayed(user1.id, player1Card),
        TrickCompleted(user2.id),
        GameCompleted(
          winnerIds = List(user1.id),
          points = List(
            PointsCount(List(user2.id), 21),
            PointsCount(List(user1.id), 99),
          ),
          matchPoints = List(
            PointsCount(List(user2.id), 1),
            PointsCount(List(user1.id), 3)
          )
        ),
        MatchCompleted(List(user1.id))
      )

      newContext shouldBe Some(GameContext(
        table = TableServerView(
          seats = List(
            Seat(
              player = Some(EndOfMatchPlayer(player1.win, winner = true)),
              hand = Nil,
              taken = Nil,
              played = Nil
            ),
            Seat(
              player = Some(EndOfMatchPlayer(player2, winner = false)),
              hand = Nil,
              taken = Nil,
              played = Nil
            )
          ),
          deck = Nil,
          board = Nil,
          active = false
        ),
        stateMachine = None
      ))

      messages shouldBe Nil
    }
  }

  "Future undelivered events will still be sent" in {

    val message1: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(user2.id, Card(Rank.Asso, Suit.Coppe))), Delay.Long)
    val message2: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(user2.id, Card(Rank.Due, Suit.Coppe))), Delay.Medium)
    val message3: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(user2.id, Card(Rank.Tre, Suit.Coppe))), Delay.Short)

    val eventsIO = (for {
      bus <- MessageBus.inMemory[IO]
      gameRepo <- JsonRepos.gameRepo
      messageRepo <- JsonRepos.messageRepo
      _ <- messageRepo.flying(message1)
      _ <- messageRepo.flying(message2)
      _ <- messageRepo.flying(message3)
      events <- (for {
        gameServiceRunner <- fs2.Stream.resource(
          GameService.runner[IO](bus, gameRepo, messageRepo, {
            case Delay.Short => 10.milli
            case Delay.Medium => 20.millis
            case Delay.Long => 30.millis
            case Delay.Tick => 100.millis
          })
        )
        subscription <- fs2.Stream.resource(bus.subscribeAwait)
        message <- subscription
          .concurrently(bus.run)
          .concurrently(gameServiceRunner)
          .interruptAfter(2.seconds)
      } yield message).compile.toList
    } yield events)

    eventsIO.asserting(_ shouldBe List(message3.inner, message2.inner, message1.inner))
  }