package bastoni.domain.logic
package scopa

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.logic.scopa.MatchState.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers


class Scopa2Spec extends AnyFreeSpec with Matchers:
  val players = List(player1, player2)

  "Cards are dealt correctly" in {
    val input = fs2.Stream[fs2.Pure, StateMachineInput](
      ShuffleDeck(shuffleSeed),
      dealCards,
      dealCards,
      dealCards,
      TakeCards(player1.id, Card(Sette, Denari), List(Card(Asso, Bastoni), Card(Sei, Bastoni))),
      TakeCards(player2.id, Card(Quattro, Spade), Nil),
      TakeCards(player1.id, Card(Asso, Spade), Nil),
      TakeCards(player2.id, Card(Sei, Denari), List(Card(Asso, Spade), Card(Cinque, Coppe))),
      TakeCards(player1.id, Card(Due, Bastoni), Nil),
      TakeCards(player2.id, Card(Re, Denari), Nil),
      dealCards,
      dealCards,

    ).map(_.toMessage(room1))

    Game.playMatch[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[StateMachineOutput](
      DeckShuffled(shuffledDeck),

      Continue.later,
      CardsDealt(player1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player),
      Continue.later,
      CardsDealt(player2.id, List(Card(Quattro, Spade), Card(Sei, Denari), Card(Re, Denari)), Direction.Player),
      Continue.later,
      BoardCardsDealt(List(Card(Cinque, Coppe), Card(Asso, Bastoni), Card(Cinque, Spade), Card(Sei, Bastoni))),

      ActionRequested(player1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-878029045),
      CardsTaken(player1.id, Card(Sette, Denari), List(Card(Sette, Denari), Card(Asso, Bastoni), Card(Sei, Bastoni)), extraPoint = false),

      ActionRequested(player2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-1385947143),
      CardsTaken(player2.id, Card(Quattro,Spade), Nil, false),

      ActionRequested(player1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(1752311865),
      CardsTaken(player1.id, Card(Asso,Spade), Nil, false),

      ActionRequested(player2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-464219601),
      CardsTaken(player2.id, Card(Sei,Denari), List(Card(Sei,Denari), Card(Asso,Spade), Card(Cinque,Coppe)), false),

      ActionRequested(player1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(-59815643),
      CardsTaken(player1.id, Card(Due,Bastoni), Nil, false),

      ActionRequested(player2.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(1258608161),
      CardsTaken(player2.id, Card(Re,Denari), Nil, false),

      Continue.later,
      CardsDealt(player1.id, List(Card(Tre,Spade), Card(Tre,Denari), Card(Asso,Coppe)), Direction.Player),
      Continue.later,
      CardsDealt(player2.id, List(Card(Fante,Bastoni), Card(Due,Denari), Card(Fante,Spade)), Direction.Player),

      ActionRequested(player1.id, Action.TakeCards, Some(Timeout.Max)),
      willTick(1793470367),
    ).map(_.toMessage(room1))
  }

  "Players cannot take two cards if they can take one only" in {
    val initialState =
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player1, 0), List(), Nil, 0),
          MatchPlayer(GamePlayer(player2, 0), List(Card(Sette, Spade)), Nil, 0)
        ),
        deck = Nil,
        board = List(
          Card(Tre, Spade),
          Card(Sette, Denari),
          Card(Quattro, Coppe)
        )
      )

    val input = TakeCards(player1.id, Card(Sette, Spade), List(Card(Tre, Spade), Card(Quattro, Coppe)))

    val (newState: MatchState, events: List[StateMachineOutput]) = Game.playMatchStep(initialState, input)

    newState shouldBe initialState
    events shouldBe Nil
  }

  "Players must take if they can" in {
    val initialState =
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player1, 0), List(), Nil, 0),
          MatchPlayer(GamePlayer(player2, 0), List(Card(Sette, Spade)), Nil, 0)
        ),
        deck = Nil,
        board = List(
          Card(Tre, Spade),
          Card(Sette, Denari),
          Card(Quattro, Coppe)
        )
      )

    val input = TakeCards(player1.id, Card(Sette, Spade), Nil)

    val (newState: MatchState, events: List[StateMachineOutput]) = Game.playMatchStep(initialState, input)

    newState shouldBe initialState
    events shouldBe Nil
  }

  "Players cannot take any card they like" in {
    val initialState =
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player1, 0), List(Card(Sette, Denari)), Nil, 0),
          MatchPlayer(GamePlayer(player2, 0), List(Card(Sette, Spade)), Nil, 0)
        ),
        deck = Nil,
        board = List(
          Card(Tre, Spade),
          Card(Quattro, Coppe),
          Card(Re, Bastoni)
        )
      )

    val input = TakeCards(player1.id, Card(Sette, Denari), List(Card(Re, Bastoni)))

    val (newState: MatchState, events: List[StateMachineOutput]) = Game.playMatchStep(initialState, input)

    newState shouldBe initialState
    events shouldBe Nil
  }

  "Players can take cards" in {
    val initialState =
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player1, 0), List(Card(Cavallo, Denari)), Nil, 0),
          MatchPlayer(GamePlayer(player2, 0), List(Card(Sette, Spade)), Nil, 0)
        ),
        deck = Nil,
        board = List(
          Card(Tre, Spade),
          Card(Due, Spade),
          Card(Quattro, Coppe),
          Card(Sei, Bastoni)
        )
      )

    val input = TakeCards(player1.id, Card(Cavallo, Denari), List(Card(Quattro, Coppe), Card(Tre, Spade), Card(Due, Spade)))

    val (newState: MatchState, events: List[StateMachineOutput]) = Game.playMatchStep(initialState, input)

    newState shouldBe WaitingForPlayer(
      -518420047,
      Timeout.Max,
      ActionRequested(player2.id, Action.TakeCards, Some(Timeout.Max)),
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player2, 0), List(Card(Sette, Spade)), Nil, 0),
          MatchPlayer(GamePlayer(player1, 0), Nil, List(Card(Cavallo, Denari), Card(Quattro, Coppe), Card(Tre, Spade), Card(Due, Spade)), 0),
        ),
        deck = Nil,
        board = List(Card(Sei, Bastoni))
      )
    )

    events shouldBe List(
      CardsTaken(
        player1.id,
        Card(Cavallo, Denari),
        List(Card(Cavallo, Denari), Card(Quattro, Coppe), Card(Tre, Spade), Card(Due, Spade)),
        extraPoint = false
      ),
      ActionRequested(player2.id, Action.TakeCards, Some(Timeout.Max)),
      Delayed(Tick(-518420047), Delay.Tick)
    )
  }

  "Players gain one point when they clear the board" in {
    val initialState =
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player1, 0), List(Card(Sette, Denari)), Nil, 0),
          MatchPlayer(GamePlayer(player2, 0), List(Card(Sette, Spade)), Nil, 0)
        ),
        deck = Nil,
        board = List(Card(Tre, Spade), Card(Quattro, Coppe))
      )

    val input = TakeCards(player1.id, Card(Sette, Denari), List(Card(Tre, Spade), Card(Quattro, Coppe)))

    val (newState: MatchState, events: List[StateMachineOutput]) = Game.playMatchStep(initialState, input)

    newState shouldBe WaitingForPlayer(
      740851846,
      Timeout.Max,
      ActionRequested(player2.id, Action.TakeCards, Some(Timeout.Max)),
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player2, 0), List(Card(Sette, Spade)), Nil, 0),
          MatchPlayer(GamePlayer(player1, 0), Nil, List(Card(Sette, Denari), Card(Tre, Spade), Card(Quattro, Coppe)), 1),
        ),
        deck = Nil,
        board = Nil
      )
    )

    events shouldBe List(
      CardsTaken(
        player1.id,
        Card(Sette, Denari),
        List(Card(Sette, Denari), Card(Tre, Spade), Card(Quattro, Coppe)),
        extraPoint = true
      ),
      ActionRequested(player2.id, Action.TakeCards, Some(Timeout.Max)),
      Delayed(Tick(740851846), Delay.Tick)
    )
  }

  "Players don't gain an extra point if they clear the board at the end of the game" in {
    val initialState =
      PlayRound(
        players = List(
          MatchPlayer(GamePlayer(player1, 0), List(Card(Sette, Denari)), Nil, 0),
          MatchPlayer(GamePlayer(player2, 0), Nil, Nil, 0)
        ),
        deck = Nil,
        board = List(Card(Tre, Spade), Card(Quattro, Coppe))
      )

    val input = TakeCards(player1.id, Card(Sette, Denari), List(Card(Tre, Spade), Card(Quattro, Coppe)))

    val (newState: MatchState, events: List[StateMachineOutput]) = Game.playMatchStep(initialState, input)

    newState shouldBe WillComplete(
      List(
        MatchPlayer(GamePlayer(player2, 0), Nil, Nil, 0),
        MatchPlayer(GamePlayer(player1, 0), Nil, List(Card(Sette, Denari), Card(Tre, Spade), Card(Quattro, Coppe)), 0),
      )
    )

    events shouldBe List(
      CardsTaken(
        player1.id,
        Card(Sette, Denari),
        List(Card(Sette, Denari), Card(Tre, Spade), Card(Quattro, Coppe)),
        extraPoint = false
      ),
      Delayed(Continue, Delay.Long)
    )
  }
