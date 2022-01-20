package bastoni.domain.logic

import bastoni.domain.AsyncIOFreeSpec
import bastoni.domain.logic.Fixtures.*
import bastoni.domain.logic.{GameContext, GamePubSub}
import bastoni.domain.model.Command.*
import bastoni.domain.model.PlayerState.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import bastoni.domain.model.*
import bastoni.domain.repos.{GameRepo, MessageRepo}
import bastoni.domain.view.FromPlayer
import cats.effect.IO

import scala.concurrent.duration.DurationInt

class GameServiceSpec extends AsyncIOFreeSpec:

  val room1Id: RoomId = RoomId.newId
  val room2Id: RoomId = RoomId.newId
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
      PlayCard(user1.id, cardOf(Rank.Sette, Suit.Denari)).toMessage(room1Id) // will be ignored
    )

    gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(user1, 3).toMessage(room1Id)))
  }

  "A new game" - {
    "cannot start if only players is at the table" in {
      val commands = fs2.Stream(
        JoinTable(user1, joinSeed),
        StartMatch(user1.id, GameType.Briscola)
      ).map(_.toMessage(room1Id))

      gameService(commands).asserting(_ shouldBe List(PlayerJoinedTable(user1, 3).toMessage(room1Id)))
    }

    "can start for 2 players" in {
      val commands = fs2.Stream(
        JoinTable(user1, joinSeed),
        JoinTable(user2, joinSeed),
        StartMatch(user1.id, GameType.Briscola)
      ).map(_.toMessage(room1Id))

      gameService(commands).asserting(_ shouldBe List(
        PlayerJoinedTable(user1, 3).toMessage(room1Id),
        PlayerJoinedTable(user2, 2).toMessage(room1Id),
        MatchStarted(GameType.Briscola, List(
          MatchScore(List(user2.id), 0),
          MatchScore(List(user1.id), 0)
        )).toMessage(room1Id)
      ))
    }

  }


  "Two simultaneous matches can be played" in {
    val inputStream = fs2.Stream(
      JoinTable(user2, joinSeed).toMessage(room1Id),
      JoinTable(user1, joinSeed).toMessage(room1Id),
      JoinTable(user3, joinSeed).toMessage(room2Id),
      JoinTable(user2, joinSeed).toMessage(room2Id),

      StartMatch(user2.id, GameType.Briscola).toMessage(room1Id),
      StartMatch(user2.id, GameType.Tressette).toMessage(room2Id),

      ShuffleDeck(shuffleSeed).toMessage(room1Id),
      Continue.toMessage(room1Id),
      ShuffleDeck(shuffleSeed).toMessage(room2Id),
      Continue.toMessage(room2Id),
      LeaveTable(user1).toMessage(room1Id),
      PlayerLeftTable(user1, 2).toMessage(room1Id),
      Continue.toMessage(room1Id)
    )

    gameService(inputStream).asserting(_ shouldBe List(
      PlayerJoinedTable(user2, 3).toMessage(room1Id),
      PlayerJoinedTable(user1, 2).toMessage(room1Id),
      PlayerJoinedTable(user3, 3).toMessage(room2Id),
      PlayerJoinedTable(user2, 2).toMessage(room2Id),

      MatchStarted(GameType.Briscola, List(MatchScore(List(user1.id), 0), MatchScore(List(user2.id), 0))).toMessage(room1Id),
      MatchStarted(GameType.Tressette, List(MatchScore(List(user2.id), 0), MatchScore(List(user3.id), 0))).toMessage(room2Id),

      DeckShuffled(shuffledDeck).toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.AfterShuffleDeck),
      CardsDealt(user1.id, List(cardOf(Due, Bastoni), cardOf(Asso, Spade), cardOf(Sette, Denari)), Direction.Player).toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.AfterDealCards),
      DeckShuffled(shuffledDeck).toMessage(room2Id),
      Delayed(Continue.toMessage(room2Id), Delay.AfterShuffleDeck),
      CardsDealt(user2.id, List(cardOf(Due, Bastoni), cardOf(Asso, Spade), cardOf(Sette, Denari), cardOf(Quattro, Spade), cardOf(Sei, Denari)), Direction.Player).toMessage(room2Id),
      Delayed(Continue.toMessage(room2Id), Delay.AfterDealCards),
      PlayerLeftTable(user1, 2).toMessage(room1Id),
      GameAborted.toMessage(room1Id),
      Delayed(Continue.toMessage(room1Id), Delay.AfterGameOver),
      MatchAborted.toMessage(room1Id),
    ))
  }

  "Snapshots are accurate" in {
    val inputStream = fs2.Stream[fs2.Pure, StateMachineInput](
      JoinTable(user2, joinSeed),
      JoinTable(user1, joinSeed),

      StartMatch(user2.id, GameType.Briscola),
      ShuffleDeck(shuffleSeed),
      Continue,
      Continue,
      Continue,
      Continue,
      Connect,
      PlayCard(user1.id, cardOf(Due, Bastoni)),
      Continue,
      Connect
    ).map(_.toMessage(room1))

    gameService(inputStream).asserting { events =>
      events.collect { case Message(_, _, s: Snapshot) => s } shouldBe
        List(
          Snapshot(
            TableServerView(
              List(
                Seat(None, Nil, Nil),
                Seat(None, Nil, Nil),
                Seat(
                  Some(
                    ActingPlayer(
                      MatchPlayer(user1, 0),
                      Action.PlayCard,
                      Some(Timeout.Max)
                    )
                  ),
                  hand = List(
                    CardServerView(cardOf(Due, Bastoni), Direction.Player),
                    CardServerView(cardOf(Asso, Spade), Direction.Player),
                    CardServerView(cardOf(Sette, Denari), Direction.Player)
                  ),
                  taken = Nil
                ),
                Seat(
                  Some(WaitingPlayer(MatchPlayer(user2, 0))),
                  hand = List(
                    CardServerView(cardOf(Quattro, Spade), Direction.Player),
                    CardServerView(cardOf(Sei, Denari), Direction.Player),
                    CardServerView(cardOf(Re, Denari), Direction.Player)
                  ),
                  taken = Nil
                )
              ),
              deck = shuffledDeck.cards.drop(7).map(card => CardServerView(card, Direction.Down)) :+ CardServerView(cardOf(Cinque, Coppe), Direction.Up),
              board = Nil,
              matchInfo = Some(MatchInfo(
                GameType.Briscola,
                List(
                  MatchScore(List(user1.id), 0),
                  MatchScore(List(user2.id), 0),
                ),
                None
              )),
              dealerIndex = None
            )
          ),
          Snapshot(
            TableServerView(
              List(
                Seat(None, Nil, Nil),
                Seat(None, Nil, Nil),
                Seat(
                  Some(WaitingPlayer(MatchPlayer(user1, 0))),
                  hand = List(
                    CardServerView(cardOf(Asso, Spade), Direction.Player),
                    CardServerView(cardOf(Sette, Denari), Direction.Player)
                  ),
                  taken = Nil
                ),
                Seat(
                  Some(ActingPlayer(MatchPlayer(user2, 0), Action.PlayCard, Some(Timeout.Max))),
                  hand = List(
                    CardServerView(cardOf(Quattro, Spade), Direction.Player),
                    CardServerView(cardOf(Sei, Denari), Direction.Player),
                    CardServerView(cardOf(Re, Denari), Direction.Player)
                  ),
                  taken = Nil
                )
              ),
              deck = shuffledDeck.cards.drop(7).map(card => CardServerView(card, Direction.Down)) :+ CardServerView(cardOf(Cinque, Coppe), Direction.Up),
              board = List(Some(user1.id) -> CardServerView(cardOf(Due, Bastoni), Direction.Up)),
              matchInfo = Some(MatchInfo(
                GameType.Briscola,
                List(
                  MatchScore(List(user1.id), 0),
                  MatchScore(List(user2.id), 0),
                ),
                None
              )),
              dealerIndex = None
            )
          )
        )

    }
  }

  "First player to join will be the dealer" in {
    val inputStream = fs2.Stream[fs2.Pure, StateMachineInput](
      JoinTable(user2, joinSeed),
      PlayerJoinedTable(user2, 3),
      JoinTable(user1, joinSeed),
      PlayerJoinedTable(user1, 2),
      Connect
    ).map(_.toMessage(room1))

    gameService(inputStream).asserting { events =>
      events.collect { case Message(_, _, s: Snapshot) => s } shouldBe
        List(
          Snapshot(
            TableServerView(
              List(
                Seat(None, Nil, Nil),
                Seat(None, Nil, Nil),
                Seat(
                  Some(SittingOut(user1)),
                  hand = Nil,
                  taken = Nil
                ),
                Seat(
                  Some(SittingOut(user2)),
                  hand = Nil,
                  taken = Nil
                )
              ),
              deck = Nil,
              board = Nil,
              matchInfo = None,
              dealerIndex = Some(3)
            )
          )
        )

    }

  }

  "A pre-existing game can be resumed and completed" in {
    val player1 = MatchPlayer(user1, 2)
    val player2 = MatchPlayer(user2, 1)

    val player1Card = cardOf(Rank.Tre, Suit.Denari)
    val player2Card = cardOf(Rank.Asso, Suit.Denari)

    val player1Collected = shuffledDeck.cards.filter(card => card != player1Card && card != player2Card)

    val initialContext = new GameContext(
      table = TableServerView(
        seats = List(
          Seat(
            player = Some(ActingPlayer(player1, Action.PlayCard, Some(Timeout.Max))),
            hand = List(CardServerView(player1Card, Direction.Player)),
            taken = player1Collected.map(card => CardServerView(card, Direction.Down))
          ),
          Seat(
            player = Some(WaitingPlayer(player2)),
            hand = Nil,
            taken = Nil
          )
        ),
        deck = Nil,
        board = List(None -> CardServerView(player2Card, Direction.Up)),
        matchInfo = Some(MatchInfo(GameType.Briscola, Nil, None)),
        dealerIndex = None
      ),
      stateMachine = Some(generic.StateMachine(
        briscola.Game,
        briscola.MatchState.InProgress(
          List(player1, player2),
          briscola.GameState.PlayRound(
            List(Player(player1, List(player1Card), player1Collected)),
            List(Player(player2, Nil, Nil) -> player2Card),
            Nil.toDeck,
            player1Card
          ),
          rounds = 0
        )
      ))
    )

    val oldMessage = CardPlayed(user2.id, player2Card).toMessage(room1Id)

    val resultIO = for {
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
    } yield (events, newContext, messages)

    resultIO.asserting { case (events, newContext, messages) =>
      events shouldBe List(
        oldMessage.data,
        CardPlayed(user1.id, player1Card),
        TrickCompleted(user2.id),
        GameCompleted(
          scores = List(
            BriscolaGameScore(
              List(user2.id),
              List(
                BriscolaGameScoreItem(cardOf(Asso, Denari), 11),
                BriscolaGameScoreItem(cardOf(Tre, Denari), 10)
              )
            ),
            BriscolaGameScore(
              List(user1.id),
              List(
                BriscolaGameScoreItem(cardOf(Asso, Spade), 11),
                BriscolaGameScoreItem(cardOf(Asso, Bastoni), 11),
                BriscolaGameScoreItem(cardOf(Asso, Coppe), 11),
                BriscolaGameScoreItem(cardOf(Tre, Spade), 10),
                BriscolaGameScoreItem(cardOf(Tre, Coppe), 10),
                BriscolaGameScoreItem(cardOf(Tre, Bastoni), 10),
                BriscolaGameScoreItem(cardOf(Re, Denari), 4),
                BriscolaGameScoreItem(cardOf(Re, Bastoni), 4),
                BriscolaGameScoreItem(cardOf(Re, Coppe), 4),
                BriscolaGameScoreItem(cardOf(Re, Spade), 4),
                BriscolaGameScoreItem(cardOf(Cavallo, Denari), 3),
                BriscolaGameScoreItem(cardOf(Cavallo, Bastoni), 3),
                BriscolaGameScoreItem(cardOf(Cavallo, Spade), 3),
                BriscolaGameScoreItem(cardOf(Cavallo, Coppe), 3),
                BriscolaGameScoreItem(cardOf(Fante, Bastoni), 2),
                BriscolaGameScoreItem(cardOf(Fante, Spade), 2),
                BriscolaGameScoreItem(cardOf(Fante, Coppe), 2),
                BriscolaGameScoreItem(cardOf(Fante, Denari), 2),
              )
            ),
          ),
          matchScores = List(
            MatchScore(List(user2.id), 1),
            MatchScore(List(user1.id), 3)
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
              taken = Nil
            ),
            Seat(
              player = Some(EndOfMatchPlayer(player2, winner = false)),
              hand = Nil,
              taken = Nil
            )
          ),
          deck = Nil,
          board = Nil,
          matchInfo = None,
          dealerIndex = None
        ),
        stateMachine = None
      ))

      messages shouldBe Nil
    }
  }

  "Future undelivered events will still be sent" in {

    val message1: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(user2.id, cardOf(Rank.Asso, Suit.Coppe))), Delay.BeforeGameOver)
    val message2: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(user2.id, cardOf(Rank.Due, Suit.Coppe))), Delay.BeforeTakeCards)
    val message3: Delayed[Message] = Delayed(Message(MessageId.newId, room1Id, CardPlayed(user2.id, cardOf(Rank.Tre, Suit.Coppe))), Delay.AfterDealCards)

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
            case Delay.AfterDealCards => 20.millis
            case Delay.BeforeTakeCards => 30.millis
            case Delay.BeforeGameOver => 40.millis
            case _ => 100.millis
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
