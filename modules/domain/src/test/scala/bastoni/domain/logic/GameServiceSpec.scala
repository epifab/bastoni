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
  val room1Players = List(player1, player2)
  val room2Players = List(player2, player3)

  def gameService(inputStream: fs2.Stream[IO, Message]): IO[List[Message | Delayed[Message]]] = for {
    gameRepo <- GameRepo.inMemory[IO]
    messageRepo <- MessageRepo.inMemory[IO]
    output <- GameService[IO](IO.pure(messageId), gameRepo, messageRepo)(inputStream).compile.toList
  } yield output

  "Rooms can be joined and left" in {
    val commands = fs2.Stream(
      JoinTable(player1, joinSeed),
      JoinTable(player2, joinSeed),
      LeaveTable(player1),
      LeaveTable(player2)
    ).map(_.toMessage(room1Id))

    gameService(commands).asserting(_ shouldBe List(
      PlayerJoinedTable(player1, 3),
      PlayerJoinedTable(player2, 2),
      PlayerLeftTable(player1, 3),
      PlayerLeftTable(player2, 2)
    ).map(_.toMessage(room1Id)))
  }

  "Players cannot join a room that is full" in {
    val commands = fs2.Stream(
      JoinTable(player1, joinSeed),
      JoinTable(player2, joinSeed),
      JoinTable(player3, joinSeed),
      JoinTable(player4, joinSeed),
      JoinTable(player5, joinSeed),
    ).map(_.toMessage(room1Id))

    gameService(commands).asserting(_ shouldBe List(
      PlayerJoinedTable(player1, 3),
      PlayerJoinedTable(player2, 2),
      PlayerJoinedTable(player3, 1),
      PlayerJoinedTable(player4, 0)
    ).map(_.toMessage(room1Id)))
  }

  "Players can join multiple rooms" in {
    val commands = fs2.Stream(
      JoinTable(player1, joinSeed).toMessage(room1Id),
      JoinTable(player1, joinSeed).toMessage(room2Id)
    )

    gameService(commands).asserting(_ shouldBe List(
      PlayerJoinedTable(player1, 3).toMessage(room1Id),
      PlayerJoinedTable(player1, 3).toMessage(room2Id)
    ))
  }

  "Messages from different rooms won't interfere" in {
    val commands = fs2.Stream(
      JoinTable(player1, joinSeed).toMessage(room1Id),
      LeaveTable(player1).toMessage(room2Id),  // will be ignored as room2 doesn't exist
    )

    gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(player1, 3).toMessage(room1Id)))
  }

  "Players cannot join the same room twice" in {
    val commands = fs2.Stream(
      JoinTable(player1, joinSeed).toMessage(room1Id),
      JoinTable(player1, joinSeed).toMessage(room1Id),  // will be ignored
    )

    gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(player1, 3).toMessage(room1Id)))
  }

  "Random messages will be ignored" in {
    val commands = fs2.Stream(
      JoinTable(player1, joinSeed).toMessage(room1Id),
      PlayCard(player1.id, Card(Rank.Sette, Suit.Denari)).toMessage(room1Id) // will be ignored
    )

    gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(player1, 3).toMessage(room1Id)))
  }

  "A new game" - {
    "cannot start if only players is at the table" in {
      val commands = fs2.Stream(
        JoinTable(player1, joinSeed),
        StartGame(player1.id, GameType.Briscola)
      ).map(_.toMessage(room1Id))

      gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(player1, 3).toMessage(room1Id)))
    }

    "can start for 2 players" in {
      val commands = fs2.Stream(
        JoinTable(player1, joinSeed),
        JoinTable(player2, joinSeed),
        StartGame(player1.id, GameType.Briscola)
      ).map(_.toMessage(room1Id))

      gameService(commands).asserting(_ shouldBe List(
        PlayerJoinedTable(player1, 3).toMessage(room1Id),
        PlayerJoinedTable(player2, 2).toMessage(room1Id),
        GameStarted(GameType.Briscola).toMessage(room1Id)
      ))
    }

  }


  "Two simultaneous matches can be played" in {
    val inputStream = fs2.Stream(
      JoinTable(player2, joinSeed).toMessage(room1Id),
      JoinTable(player1, joinSeed).toMessage(room1Id),
      JoinTable(player3, joinSeed).toMessage(room2Id),
      JoinTable(player2, joinSeed).toMessage(room2Id),

      StartGame(player2.id, GameType.Briscola).toMessage(room1Id),
      StartGame(player2.id, GameType.Tressette).toMessage(room2Id),

      ShuffleDeck(shuffleSeed).toMessage(room1Id),
      Continue.toMessage(room1Id),
      ShuffleDeck(shuffleSeed).toMessage(room2Id),
      Continue.toMessage(room1Id),
      Continue.toMessage(room2Id),
      LeaveTable(player1).toMessage(room1Id),
      PlayerLeftTable(player1, 2).toMessage(room1Id)
    )

    gameService(inputStream).asserting(_ shouldBe List(
      PlayerJoinedTable(player2, 3).toMessage(room1Id),
      PlayerJoinedTable(player1, 2).toMessage(room1Id),
      PlayerJoinedTable(player3, 3).toMessage(room2Id),
      PlayerJoinedTable(player2, 2).toMessage(room2Id),

      GameStarted(GameType.Briscola).toMessage(room1Id),
      GameStarted(GameType.Tressette).toMessage(room2Id),

      DeckShuffled(shuffledDeck).toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.Medium),
      CardDealt(player1.id, Card(Due, Bastoni), Face.Player).toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.Short),
      DeckShuffled(shuffledDeck).toMessage(room2Id),
      Delayed(Continue.toMessage(room2Id), Delay.Medium),
      CardDealt(player2.id, Card(Asso,Spade), Face.Player).toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.Short),
      CardDealt(player2.id, Card(Due, Bastoni), Face.Player).toMessage(room2Id),
      Delayed(Continue.toMessage(room2Id), Delay.Short),
      PlayerLeftTable(player1, 2).toMessage(room1Id),
      MatchAborted.toMessage(room1Id),
      GameAborted.toMessage(room1Id),
    ))
  }

  "A complete game can be played" in {
    val inputStream =
      fs2.Stream[fs2.Pure, ServerEvent | Command](
        JoinTable(player3, joinSeed),
        JoinTable(player2, joinSeed),
        JoinTable(player1, joinSeed),
        StartGame(player1.id, GameType.Briscola),
        GameStarted(GameType.Briscola)
      ).map(_.toMessage(room1Id)) ++
      Briscola3Spec.input(room1Id, player1, player2, player3) ++
      Briscola3Spec.input(room1Id, player2, player3, player1) ++
      Briscola3Spec.input(room1Id, player3, player1, player2) ++
      Briscola3Spec.input(room1Id, player1, player2, player3) ++
      fs2.Stream(Continue.toMessage(room1Id))

    val outputStream =
      List[ServerEvent | Command](
        PlayerJoinedTable(player3, 3),
        PlayerJoinedTable(player2, 2),
        PlayerJoinedTable(player1, 1),
        GameStarted(GameType.Briscola),
      ).map(_.toMessage(room1Id)) ++
      (
        ActionRequested(player3.id, Action.ShuffleDeck).toMessage(room1Id) ::
        Briscola3Spec.output(room1Id, GamePlayer(player1, 0), GamePlayer(player2, 0), GamePlayer(player3, 0))
      ) ++
      (
        ActionRequested(player1.id, Action.ShuffleDeck).toMessage(room1Id) ::
        Briscola3Spec.output(room1Id, GamePlayer(player2, 0), GamePlayer(player3, 0), GamePlayer(player1, 1))
      ) ++
      (
        ActionRequested(player2.id, Action.ShuffleDeck).toMessage(room1Id) ::
        Briscola3Spec.output(room1Id, GamePlayer(player3, 0), GamePlayer(player1, 1), GamePlayer(player2, 1))
      ) ++
      (
        ActionRequested(player3.id, Action.ShuffleDeck).toMessage(room1Id) ::
        Briscola3Spec.output(room1Id, GamePlayer(player1, 1), GamePlayer(player2, 1), GamePlayer(player3, 1))
      ) ++
      List(GameCompleted(List(player1.id)).toMessage(room1Id))

    gameService(inputStream).asserting(_ shouldBe outputStream)
  }

  "A pre-existing game can be resumed and completed" in {
    val gamePlayer1 = GamePlayer(player1, 2)
    val gamePlayer2 = GamePlayer(player2, 1)

    val player1Card = Card(Rank.Tre, Suit.Denari)
    val player2Card = Card(Rank.Asso, Suit.Denari)

    val player1Collected = Deck.instance.filter(card => card != player1Card && card != player2Card)

    val initialContext = new GameContext(
      table = new TableServerView(
        seats = List(
          Seat(
            player = Some(ActingPlayer(gamePlayer1, Action.PlayCard)),
            hand = List(CardServerView(player1Card, Face.Player)),
            collected = player1Collected.map(card => CardServerView(card, Face.Down)),
            played = Nil
          ),
          Seat(
            player = Some(WaitingPlayer(gamePlayer2)),
            hand = Nil,
            collected = Nil,
            played = List(CardServerView(player2Card, Face.Up))
          )
        ),
        deck = Nil,
        active = true
      ),
      stateMachine = Some(new briscola.StateMachine(
        briscola.GameState.InProgress(
          List(gamePlayer1, gamePlayer2),
          briscola.MatchState.PlayRound(
            List(MatchPlayer(gamePlayer1, Set(player1Card), player1Collected.toSet)),
            List(MatchPlayer(gamePlayer2, Set.empty, Set.empty) -> player2Card),
            Nil,
            player1Card
          ),
          rounds = 0
        )
      ))
    )

    val oldMessage = CardPlayed(player2.id, player2Card).toMessage(room1Id)

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
              .through(GamePubSub.publisher(messageBus).publish(player1, room1Id))
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
        CardPlayed(player1.id, player1Card),
        TrickCompleted(player2.id),
        MatchCompleted(
          winnerIds = List(player1.id),
          matchPoints = List(
            PointsCount(List(player2.id), 21),
            PointsCount(List(player1.id), 99),
          ),
          gamePoints = List(
            PointsCount(List(player2.id), 1),
            PointsCount(List(player1.id), 3)
          )
        ),
        GameCompleted(List(player1.id))
      )

      newContext shouldBe Some(GameContext(
        table = TableServerView(
          seats = List(
            Seat(
              player = Some(EndOfGamePlayer(gamePlayer1.win, winner = true)),
              hand = Nil,
              collected = Nil,
              played = Nil
            ),
            Seat(
              player = Some(EndOfGamePlayer(gamePlayer2, winner = false)),
              hand = Nil,
              collected = Nil,
              played = Nil
            )
          ),
          deck = Nil,
          active = false
        ),
        stateMachine = None
      ))

      messages shouldBe Nil
    }
  }

  "Future undelivered events will still be sent" in {

    val message1: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(player2.id, Card(Rank.Asso, Suit.Coppe))), Delay.Long)
    val message2: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(player2.id, Card(Rank.Due, Suit.Coppe))), Delay.Medium)
    val message3: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(player2.id, Card(Rank.Tre, Suit.Coppe))), Delay.Short)

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
