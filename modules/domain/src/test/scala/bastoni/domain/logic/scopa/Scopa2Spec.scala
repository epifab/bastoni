package bastoni.domain.logic
package scopa

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.logic.scopa.ScopaGameState.*
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
      Continue,
      TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Asso, Bastoni), cardOf(Sei, Bastoni))),
      Continue,
      Continue,
      TakeCards(user2.id, cardOf(Quattro, Spade), Nil),
      Continue,
      Continue,
      TakeCards(user1.id, cardOf(Asso, Spade), Nil),
      Continue,
      Continue,
      TakeCards(user2.id, cardOf(Sei, Denari), List(cardOf(Asso, Spade), cardOf(Cinque, Coppe))),
      Continue,
      Continue,
      TakeCards(user1.id, cardOf(Due, Bastoni), Nil),
      Continue,
      Continue,
      TakeCards(user2.id, cardOf(Re, Denari), Nil),
      Continue,
      Continue,
      Continue,
      Continue,
    )

    ScopaGame.playStream[cats.Id](players)(input).compile.toList shouldBe List[StateMachineOutput](
      DeckShuffled(shuffledDeck),
      Continue.afterShufflingDeck,

      CardsDealt(user1.id, List(cardOf(Due, Bastoni), cardOf(Asso, Spade), cardOf(Sette, Denari)), Direction.Player),
      Continue.afterDealingCards,
      CardsDealt(user2.id, List(cardOf(Quattro, Spade), cardOf(Sei, Denari), cardOf(Re, Denari)), Direction.Player),
      Continue.afterDealingCards,
      BoardCardsDealt(List(cardOf(Cinque, Coppe), cardOf(Asso, Bastoni), cardOf(Cinque, Spade), cardOf(Sei, Bastoni))),
      Continue.afterDealingCards,

      Act(user1.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(1215977239),
      CardPlayed(user1.id, cardOf(Sette, Denari)),
      Continue.beforeTakingCards,
      CardsTaken(user1.id, List(cardOf(Sette, Denari), cardOf(Asso, Bastoni), cardOf(Sei, Bastoni)), scopa = None),
      Continue.afterTakingCards,

      Act(user2.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(1358241511),
      CardPlayed(user2.id, cardOf(Quattro, Spade)),
      Continue.beforeTakingCards,
      CardsTaken(user2.id, Nil, None),
      Continue.afterTakingCards,

      Act(user1.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(-161735769),
      CardPlayed(user1.id, cardOf(Asso, Spade)),
      Continue.beforeTakingCards,
      CardsTaken(user1.id, Nil, None),
      Continue.afterTakingCards,

      Act(user2.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(121586655),
      CardPlayed(user2.id, cardOf(Sei,Denari)),
      Continue.beforeTakingCards,
      CardsTaken(user2.id, List(cardOf(Sei,Denari), cardOf(Asso,Spade), cardOf(Cinque,Coppe)), None),
      Continue.afterTakingCards,

      Act(user1.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(413378932),
      CardPlayed(user1.id, cardOf(Due,Bastoni)),
      Continue.beforeTakingCards,
      CardsTaken(user1.id, Nil, None),
      Continue.afterTakingCards,

      Act(user2.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(1414415910),
      CardPlayed(user2.id, cardOf(Re,Denari)),
      Continue.beforeTakingCards,
      CardsTaken(user2.id, Nil, None),
      Continue.afterTakingCards,

      CardsDealt(user1.id, List(cardOf(Tre,Spade), cardOf(Tre,Denari), cardOf(Asso,Coppe)), Direction.Player),
      Continue.afterDealingCards,
      CardsDealt(user2.id, List(cardOf(Fante,Bastoni), cardOf(Due,Denari), cardOf(Fante,Spade)), Direction.Player),
      Continue.afterDealingCards,

      Act(user1.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(1364654232),
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
        ),
        lastTake = None
      )

    val input = TakeCards(user1.id, cardOf(Sette, Spade), List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)))

    val (newState: ScopaGameState, events: List[StateMachineOutput]) = ScopaGame.playGameStep(initialState, input)

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
        ),
        lastTake = None
      )

    val input = TakeCards(user1.id, cardOf(Sette, Spade), Nil)

    val (newState: ScopaGameState, events: List[StateMachineOutput]) = ScopaGame.playGameStep(initialState, input)

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
        ),
        lastTake = None
      )

    val input = TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Re, Bastoni)))

    val (newState: ScopaGameState, events: List[StateMachineOutput]) = ScopaGame.playGameStep(initialState, input)

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
        ),
        lastTake = None
      )

    val takeCards = TakeCards(user1.id, cardOf(Cavallo, Denari), List(cardOf(Quattro, Coppe), cardOf(Tre, Spade), cardOf(Due, Spade)))

    val (state1, e1) = ScopaGame.playGameStep(initialState, takeCards)
    val (state2, e2: List[StateMachineOutput]) = ScopaGame.playGameStep(state1, Continue)
    val (newState, e3) = ScopaGame.playGameStep(state2, Continue)

    newState shouldBe WaitingForPlayer(
      -1046079801,
      Timeout.Max,
      Act(user2.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      PlayRound(
        players = List(
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0),
          Player(MatchPlayer(user1, 0), Nil, List(cardOf(Cavallo, Denari), cardOf(Quattro, Coppe), cardOf(Tre, Spade), cardOf(Due, Spade)), 0),
        ),
        deck = Nil.toDeck,
        board = List(cardOf(Sei, Bastoni)),
        lastTake = Some(user1.id)
      )
    )

    (e1 ++ e2 ++ e3) shouldBe List(
      CardPlayed(
        user1.id,
        cardOf(Cavallo, Denari)
      ),
      Continue.beforeTakingCards,
      CardsTaken(
        user1.id,
        List(cardOf(Cavallo, Denari), cardOf(Quattro, Coppe), cardOf(Tre, Spade), cardOf(Due, Spade)),
        scopa = None
      ),
      Continue.afterTakingCards,
      Act(user2.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(-1046079801)
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
        board = List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)),
        lastTake = None
      )

    val input = TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)))

    val (state1, e1) = ScopaGame.playGameStep(initialState, input)
    val (state2, e2) = ScopaGame.playGameStep(state1, Continue)
    val (finalState, e3) = ScopaGame.playGameStep(state2, Continue)

    finalState shouldBe WaitingForPlayer(
      -1379981441,
      Timeout.Max,
      Act(user2.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      PlayRound(
        players = List(
          Player(MatchPlayer(user2, 0), List(cardOf(Sette, Spade)), Nil, 0),
          Player(MatchPlayer(user1, 0), Nil, List(cardOf(Sette, Denari), cardOf(Tre, Spade), cardOf(Quattro, Coppe)), 1),
        ),
        deck = Nil.toDeck,
        board = Nil,
        lastTake = Some(user1.id)
      )
    )

    (e1 ++ e2 ++ e3) shouldBe List(
      CardPlayed(
        user1.id,
        cardOf(Sette, Denari)
      ),
      Continue.beforeTakingCards,
      CardsTaken(
        user1.id,
        List(cardOf(Sette, Denari), cardOf(Tre, Spade), cardOf(Quattro, Coppe)),
        scopa = Some(cardOf(Sette, Denari))
      ),
      Continue.afterTakingCards,
      Act(user2.id, Action.PlayCard(PlayContext.Scopa), Some(Timeout.Max)),
      willTick(-1379981441)
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
        board = List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)),
        lastTake = None
      )

    val input = TakeCards(user1.id, cardOf(Sette, Denari), List(cardOf(Tre, Spade), cardOf(Quattro, Coppe)))

    val (newState: ScopaGameState, _) = ScopaGame.playGameStep(initialState, input)
    val (newerState: ScopaGameState, events: List[StateMachineOutput]) = ScopaGame.playGameStep(newState, Continue)

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
      Continue.beforeGameOver
    )
  }

  "Remaining cards on the board" - {
    "are collected by the player who last took some cards" in {
      val initialState =
        PlayRound(
          players = List(
            Player(MatchPlayer(user2, 0), List(cardOf(Sei, Spade)), Nil, 0),
            Player(MatchPlayer(user1, 0), List(), Nil, 0)
          ),
          deck = Nil.toDeck,
          board = List(cardOf(Tre, Spade)),
          lastTake = Some(user1.id)
        )

      val input = TakeCards(user2.id, cardOf(Sei, Spade), Nil)

      val (state1, e1) = ScopaGame.playGameStep(initialState, input)
      val (newState, e2) = ScopaGame.playGameStep(state1, Continue)

      newState shouldBe WillComplete(List(
        Player(MatchPlayer(user1, 0), Nil, List(cardOf(Sei, Spade), cardOf(Tre, Spade))),
        Player(MatchPlayer(user2, 0), Nil, Nil)
      ))
    }

    "are collected by this player if he takes some cards" in {
      val initialState =
        PlayRound(
          players = List(
            Player(MatchPlayer(user2, 0), List(cardOf(Sei, Spade)), Nil, 0),
            Player(MatchPlayer(user1, 0), List(), Nil, 0)
          ),
          deck = Nil.toDeck,
          board = List(cardOf(Tre, Spade)),
          lastTake = Some(user2.id)
        )

      val input = TakeCards(user2.id, cardOf(Sei, Spade), Nil)

      val (state1, e1) = ScopaGame.playGameStep(initialState, input)
      val (newState, e2) = ScopaGame.playGameStep(state1, Continue)

      newState shouldBe WillComplete(List(
        Player(MatchPlayer(user1, 0), Nil, Nil),
        Player(MatchPlayer(user2, 0), Nil, List(cardOf(Sei, Spade), cardOf(Tre, Spade)))
      ))
    }

    "are not collected if nobody has ever taken any card (probably not a real scenario)" in {
      val initialState =
        PlayRound(
          players = List(
            Player(MatchPlayer(user2, 0), List(cardOf(Sei, Spade)), Nil, 0),
            Player(MatchPlayer(user1, 0), List(), Nil, 0)
          ),
          deck = Nil.toDeck,
          board = List(cardOf(Tre, Spade)),
          lastTake = None
        )

      val input = TakeCards(user2.id, cardOf(Sei, Spade), Nil)

      val (state1, e1) = ScopaGame.playGameStep(initialState, input)
      val (newState, e2) = ScopaGame.playGameStep(state1, Continue)

      newState shouldBe WillComplete(List(
        Player(MatchPlayer(user1, 0), Nil, Nil),
        Player(MatchPlayer(user2, 0), Nil, Nil)
      ))
    }

  }

