package bastoni.domain.logic
package scopa

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.logic.scopa.GameState.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Delay.syntax.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


class Scopa2Spec extends AnyFreeSpec with Matchers:
  val players = List(user1, user2)

  "Cards are dealt correctly" in {
    val input = fs2.Stream[fs2.Pure, StateMachineInput](
      ShuffleDeck(shuffleSeed),
      Continue,
      Continue,
      Continue,
      TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Asso, Bastoni), cardOf(Sei, Bastoni))),
      Continue,
      TakeCards(user2.id, cardOf(Quattro, Spade), Nil),
      Continue,
      TakeCards(user1.id, cardOf(Asso, Spade), Nil),
      Continue,
      TakeCards(user2.id, cardOf(Sei, Denari), List(cardOf(Asso, Spade), cardOf(Cinque, Coppe))),
      Continue,
      TakeCards(user1.id, cardOf(Due, Bastoni), Nil),
      Continue,
      TakeCards(user2.id, cardOf(Re, Denari), Nil),
      Continue,
      Continue,
      Continue,
    )

    Game.playStream[cats.Id](players)(input).compile.toList shouldBe List[StateMachineOutput](
      DeckShuffled(shuffledDeck),

      Continue.toDealCards,
      CardsDealt(user1.id, List(cardOf(Due, Bastoni), cardOf(Asso, Spade), cardOf(Sette, Denari)), Direction.Player),
      Continue.toDealCards,
      CardsDealt(user2.id, List(cardOf(Quattro, Spade), cardOf(Sei, Denari), cardOf(Re, Denari)), Direction.Player),
      Continue.toDealCards,
      BoardCardsDealt(List(cardOf(Cinque, Coppe), cardOf(Asso, Bastoni), cardOf(Cinque, Spade), cardOf(Sei, Bastoni))),

      ActionRequested(user1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-110145523),
      CardPlayed(user1.id, cardOf(Sette, Denari)),
      Continue.toTakeCards,
      CardsTaken(user1.id, List(cardOf(Sette, Denari), cardOf(Asso, Bastoni), cardOf(Sei, Bastoni)), scopa = None),

      ActionRequested(user2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-2020154127),
      CardPlayed(user2.id, cardOf(Quattro, Spade)),
      Continue.toTakeCards,
      CardsTaken(user2.id, Nil, None),

      ActionRequested(user1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(1043339377),
      CardPlayed(user1.id, cardOf(Asso, Spade)),
      Continue.toTakeCards,
      CardsTaken(user1.id, Nil, None),

      ActionRequested(user2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(495152213),
      CardPlayed(user2.id, cardOf(Sei,Denari)),
      Continue.toTakeCards,
      CardsTaken(user2.id, List(cardOf(Sei,Denari), cardOf(Asso,Spade), cardOf(Cinque,Coppe)), None),

      ActionRequested(user1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(1767863701),
      CardPlayed(user1.id, cardOf(Due,Bastoni)),
      Continue.toTakeCards,
      CardsTaken(user1.id, Nil, None),

      ActionRequested(user2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-130872219),
      CardPlayed(user2.id, cardOf(Re,Denari)),
      Continue.toTakeCards,
      CardsTaken(user2.id, Nil, None),

      Continue.toDealCards,
      CardsDealt(user1.id, List(cardOf(Tre,Spade), cardOf(Tre,Denari), cardOf(Asso,Coppe)), Direction.Player),
      Continue.toDealCards,
      CardsDealt(user2.id, List(cardOf(Fante,Bastoni), cardOf(Due,Denari), cardOf(Fante,Spade)), Direction.Player),

      ActionRequested(user1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-972199103),
    )
  }

  "Players cannot take two cards if they can take one only" in {
    val initialState =
      PlayRound(
        players = List(
          Player(MatchPlayer(user1, 0), List(), Nil, 0),
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0)
        ),
        deck = Nil.toDeck,
        board = List(
          cardOf(Tre, Spade),
          cardOf(Sette, Denari),
          cardOf(Quattro, Coppe)
        )
      )

    val input = TakeCards(user1.id, cardOf(Sette, Spade), List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)))

    val (newState: GameState, events: List[StateMachineOutput]) = Game.playGameStep(initialState, input)

    newState shouldBe initialState
    events shouldBe Nil
  }

  "Players must take if they can" in {
    val initialState =
      PlayRound(
        players = List(
          Player(MatchPlayer(user1, 0), List(), Nil, 0),
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0)
        ),
        deck = Nil.toDeck,
        board = List(
          cardOf(Tre, Spade),
          cardOf(Sette, Denari),
          cardOf(Quattro, Coppe)
        )
      )

    val input = TakeCards(user1.id, cardOf(Sette, Spade), Nil)

    val (newState: GameState, events: List[StateMachineOutput]) = Game.playGameStep(initialState, input)

    newState shouldBe initialState
    events shouldBe Nil
  }

  "Players cannot take any card they like" in {
    val initialState =
      PlayRound(
        players = List(
          Player(MatchPlayer(user1, 0), List(cardOf(Sette, Denari)), Nil, 0),
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0)
        ),
        deck = Nil.toDeck,
        board = List(
          cardOf(Tre, Spade),
          cardOf(Quattro, Coppe),
          cardOf(Re, Bastoni)
        )
      )

    val input = TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Re, Bastoni)))

    val (newState: GameState, events: List[StateMachineOutput]) = Game.playGameStep(initialState, input)

    newState shouldBe initialState
    events shouldBe Nil
  }

  "Players can take cards" in {
    val initialState =
      PlayRound(
        players = List(
          Player(MatchPlayer(user1, 0), List(cardOf(Cavallo, Denari)), Nil, 0),
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0)
        ),
        deck = Nil.toDeck,
        board = List(
          cardOf(Tre, Spade),
          cardOf(Due, Spade),
          cardOf(Quattro, Coppe),
          cardOf(Sei, Bastoni)
        )
      )

    val input = TakeCards(user1.id, cardOf(Cavallo, Denari), List(cardOf(Quattro, Coppe), cardOf(Tre, Spade), cardOf(Due, Spade)))

    val (newState: GameState, _) = Game.playGameStep(initialState, input)
    val (newerState: GameState, events: List[StateMachineOutput]) = Game.playGameStep(newState, Continue)

    newerState shouldBe WaitingForPlayer(
      1574812349,
      Timeout.Max,
      ActionRequested(user2.id, Action.TakeCards, Some(Timeout.Max)),
      PlayRound(
        players = List(
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0),
          Player(MatchPlayer(user1, 0), Nil, List(cardOf(Cavallo, Denari), cardOf(Quattro, Coppe), cardOf(Tre, Spade), cardOf(Due, Spade)), 0),
        ),
        deck = Nil.toDeck,
        board = List(cardOf(Sei, Bastoni))
      )
    )

    events shouldBe List(
      CardsTaken(
        user1.id,
        List(cardOf(Cavallo, Denari), cardOf(Quattro, Coppe), cardOf(Tre, Spade), cardOf(Due, Spade)),
        scopa = None
      ),
      ActionRequested(user2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(1574812349)
    )
  }

  "Players gain one point when they clear the board" in {
    val initialState =
      PlayRound(
        players = List(
          Player(MatchPlayer(user1, 0), List(cardOf(Sette, Denari)), Nil, 0),
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0)
        ),
        deck = Nil.toDeck,
        board = List(cardOf(Tre, Spade), cardOf(Quattro, Coppe))
      )

    val input = TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)))

    val (newState: GameState, _) = Game.playGameStep(initialState, input)
    val (newerState: GameState, events: List[StateMachineOutput]) = Game.playGameStep(newState, Continue)

    newerState shouldBe WaitingForPlayer(
      257352564,
      Timeout.Max,
      ActionRequested(user2.id, Action.TakeCards, Some(Timeout.Max)),
      PlayRound(
        players = List(
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0),
          Player(MatchPlayer(user1, 0), Nil, List(cardOf(Sette, Denari), cardOf(Tre, Spade), cardOf(Quattro, Coppe)), 1),
        ),
        deck = Nil.toDeck,
        board = Nil
      )
    )

    events shouldBe List(
      CardsTaken(
        user1.id,
        List(cardOf(Sette, Denari), cardOf(Tre, Spade), cardOf(Quattro, Coppe)),
        scopa = Some(cardOf(Sette, Denari))
      ),
      ActionRequested(user2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(257352564)
    )
  }

  "Players don't gain an extra point if they clear the board at the end of the game" in {
    val initialState =
      PlayRound(
        players = List(
          Player(MatchPlayer(user1, 0), List(cardOf(Sette, Denari)), Nil, 0),
          Player(MatchPlayer(user2, 0), Nil, Nil, 0)
        ),
        deck = Nil.toDeck,
        board = List(cardOf(Tre, Spade), cardOf(Quattro, Coppe))
      )

    val input = TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)))

    val (newState: GameState, _) = Game.playGameStep(initialState, input)
    val (newerState: GameState, events: List[StateMachineOutput]) = Game.playGameStep(newState, Continue)

    newerState shouldBe WillComplete(
      List(
        Player(MatchPlayer(user2, 0), Nil, Nil, 0),
        Player(MatchPlayer(user1, 0), Nil, List(cardOf(Sette, Denari), cardOf(Tre, Spade), cardOf(Quattro, Coppe)), 0),
      )
    )

    events shouldBe List(
      CardsTaken(
        user1.id,
        List(cardOf(Sette, Denari), cardOf(Tre, Spade), cardOf(Quattro, Coppe)),
        scopa = None
      ),
      Continue.toCompleteGame
    )
  }
