package bastoni.domain.logic
package briscola

import bastoni.domain.logic
import bastoni.domain.logic.Fixtures.*
import bastoni.domain.logic.briscola.Game
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import cats.catsInstancesForId
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola2Spec extends AnyFreeSpec with Matchers:

  val players = List(user1, user2)

  "A game can be played" ignore {
    val input =
      fs2.Stream(
        ShuffleDeck(shuffleSeed),

        drawCards,
        drawCards,
        drawCards,
        drawCards,
        drawCards,
        drawCards,
        revealTrump,

        PlayCard(user1.id, Card(Due, Bastoni)),
        PlayCard(user2.id, Card(Quattro, Spade)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Sei, Denari)),
        PlayCard(user2.id, Card(Re, Denari)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Cinque, Spade)),
        PlayCard(user1.id, Card(Tre, Spade)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Sette, Denari)),
        PlayCard(user2.id, Card(Sei, Bastoni)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Fante, Bastoni)),
        PlayCard(user2.id, Card(Due, Denari)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Tre, Denari)),
        PlayCard(user2.id, Card(Asso, Coppe)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Sette, Bastoni)),
        PlayCard(user1.id, Card(Asso, Bastoni)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Fante, Spade)),
        PlayCard(user2.id, Card(Asso, Spade)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Cinque, Bastoni)),
        PlayCard(user1.id, Card(Cavallo, Denari)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Re, Bastoni)),
        PlayCard(user1.id, Card(Due, Coppe)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Fante, Denari)),
        PlayCard(user2.id, Card(Cavallo, Bastoni)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Quattro, Bastoni)),
        PlayCard(user2.id, Card(Cavallo, Spade)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Quattro, Coppe)),
        PlayCard(user2.id, Card(Sei, Coppe)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Sette, Spade)),
        PlayCard(user1.id, Card(Cinque, Denari)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Sette, Coppe)),
        PlayCard(user1.id, Card(Re, Spade)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Sei, Spade)),
        PlayCard(user1.id, Card(Quattro, Denari)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user2.id, Card(Tre, Bastoni)),
        PlayCard(user1.id, Card(Fante, Coppe)),
        completeTrick,

        drawCards,
        drawCards,
        PlayCard(user1.id, Card(Due, Spade)),
        PlayCard(user2.id, Card(Asso, Denari)),
        completeTrick,

        PlayCard(user1.id, Card(Cavallo, Coppe)),
        PlayCard(user2.id, Card(Re, Coppe)),
        completeTrick,

        PlayCard(user2.id, Card(Cinque, Coppe)),
        PlayCard(user1.id, Card(Tre, Coppe)),
        completeTrick,
        completeMatch,

      ).map(_.toMessage(room1))

    Game.playGame[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(user1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(user2.id, List(Card(Quattro, Spade), Card(Sei, Denari), Card(Re, Denari)), Direction.Player),
      mediumDelay,
      TrumpRevealed(Card(Cinque, Coppe)),

      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1696831491),
      CardPlayed(user1.id, Card(Due, Bastoni)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(1227817415),
      CardPlayed(user2.id, Card(Quattro, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),  // 0

      mediumDelay,
      CardsDealt(user1.id, List(Card(Asso, Bastoni)), Direction.Player),     // Sette Denari, Sei Denari, Asso Bastoni
      shortDelay,
      CardsDealt(user2.id, List(Card(Cinque, Spade)), Direction.Player),     // Asso Spade, Re Denari, Cinque spade
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1557019883),
      CardPlayed(user1.id, Card(Sei, Denari)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(-1519423663),
      CardPlayed(user2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(user2.id),  // 4

      mediumDelay,
      CardsDealt(user2.id, List(Card(Sei, Bastoni)), Direction.Player),      // Asso Spade, Cinque Spade, Sei Bastoni
      shortDelay,
      CardsDealt(user1.id, List(Card(Tre, Spade)), Direction.Player),        // Sette Denari, Asso Bastoni, Tre Spade
      ActionRequested(user2.id, Action.PlayCard),
      willTick(-485809279),
      CardPlayed(user2.id, Card(Cinque, Spade)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1610431497),
      CardPlayed(user1.id, Card(Tre, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),  // 10

      mediumDelay,
      CardsDealt(user1.id, List(Card(Tre, Denari)), Direction.Player),       // Sette Denari, Asso Bastoni, Tre Denari
      shortDelay,
      CardsDealt(user2.id, List(Card(Asso, Coppe)), Direction.Player),       // Asso Spade, Sei Bastoni, Asso Coppe
      ActionRequested(user1.id, Action.PlayCard),
      willTick(2118766723),
      CardPlayed(user1.id, Card(Sette, Denari)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(-1786131071),
      CardPlayed(user2.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickCompleted(user1.id),  // 10

      mediumDelay,
      CardsDealt(user1.id, List(Card(Fante, Bastoni)), Direction.Player),    // Asso Bastoni, Tre Denari, Fante Bastoni
      shortDelay,
      CardsDealt(user2.id, List(Card(Due, Denari)), Direction.Player),       // Asso Spade, Asso Coppe, Due Denari
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-615401723),
      CardPlayed(user1.id, Card(Fante, Bastoni)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(150476751),
      CardPlayed(user2.id, Card(Due, Denari)),
      mediumDelay,
      TrickCompleted(user1.id),  // 12

      mediumDelay,
      CardsDealt(user1.id, List(Card(Fante, Spade)), Direction.Player),      // Asso Bastoni, Tre Denari, Fante Spade
      shortDelay,
      CardsDealt(user2.id, List(Card(Re, Bastoni)), Direction.Player),       // Asso Spade, Asso Coppe, Re Bastoni
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1415975155),
      CardPlayed(user1.id, Card(Tre, Denari)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(-722753605),
      CardPlayed(user2.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(user2.id),  // 25

      mediumDelay,
      CardsDealt(user2.id, List(Card(Sette, Bastoni)), Direction.Player),    // Asso Spade, Re Bastoni, Sette Bastoni
      shortDelay,
      CardsDealt(user1.id, List(Card(Tre, Coppe)), Direction.Player),        // Asso Bastoni, Fante Spade, Tre Coppe
      ActionRequested(user2.id, Action.PlayCard),
      willTick(-888680513),
      CardPlayed(user2.id, Card(Sette, Bastoni)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1750257595),
      CardPlayed(user1.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(user1.id),  // 23

      mediumDelay,
      CardsDealt(user1.id, List(Card(Fante, Coppe)), Direction.Player),      // Fante Spade, Tre Coppe, Fante Coppe
      shortDelay,
      CardsDealt(user2.id, List(Card(Cinque, Bastoni)), Direction.Player),   // Asso Spade, Re Bastoni, Cinque Bastoni
      ActionRequested(user1.id, Action.PlayCard),
      willTick(1086954241),
      CardPlayed(user1.id, Card(Fante, Spade)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(1909131905),
      CardPlayed(user2.id, Card(Asso, Spade)),
      mediumDelay,
      TrickCompleted(user2.id),  // 38

      mediumDelay,
      CardsDealt(user2.id, List(Card(Sei, Coppe)), Direction.Player),        // Re Bastoni, Cinque Bastoni, Sei Coppe
      shortDelay,
      CardsDealt(user1.id, List(Card(Cavallo, Denari)), Direction.Player),   // Tre Coppe, Fante Coppe, Cavallo Denari
      ActionRequested(user2.id, Action.PlayCard),
      willTick(-1984889977),
      CardPlayed(user2.id, Card(Cinque, Bastoni)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-824350673),
      CardPlayed(user1.id, Card(Cavallo, Denari)),
      mediumDelay,
      TrickCompleted(user2.id),  // 41

      mediumDelay,
      CardsDealt(user2.id, List(Card(Cavallo, Bastoni)), Direction.Player),  // Re Bastoni, Sei Coppe, Cavallo Bastoni
      shortDelay,
      CardsDealt(user1.id, List(Card(Due, Coppe)), Direction.Player),        // Tre Coppe, Fante Coppe, Due Coppe
      ActionRequested(user2.id, Action.PlayCard),
      willTick(69503093),
      CardPlayed(user2.id, Card(Re, Bastoni)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(32728529),
      CardPlayed(user1.id, Card(Due, Coppe)),
      mediumDelay,
      TrickCompleted(user1.id),  // 27

      mediumDelay,
      CardsDealt(user1.id, List(Card(Fante, Denari)), Direction.Player),     // Tre Coppe, Fante Coppe, Fante Denari
      shortDelay,
      CardsDealt(user2.id, List(Card(Cavallo, Spade)), Direction.Player),    // Sei Coppe, Cavallo Bastoni, Cavallo Spade
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-88664045),
      CardPlayed(user1.id, Card(Fante, Denari)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(300625131),
      CardPlayed(user2.id, Card(Cavallo, Bastoni)),
      mediumDelay,
      TrickCompleted(user1.id),  // 32

      mediumDelay,
      CardsDealt(user1.id, List(Card(Quattro, Bastoni)), Direction.Player), // Tre Coppe, Fante Coppe, Quattro Bastoni
      shortDelay,
      CardsDealt(user2.id, List(Card(Re, Coppe)), Direction.Player),        // Sei Coppe, Cavallo Spade, Re Coppe
      ActionRequested(user1.id, Action.PlayCard),
      willTick(280019095),
      CardPlayed(user1.id, Card(Quattro, Bastoni)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(276103007),
      CardPlayed(user2.id, Card(Cavallo, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),  // 35

      mediumDelay,
      CardsDealt(user1.id, List(Card(Quattro, Coppe)), Direction.Player),   // Tre Coppe, Fante Coppe, Quattro Coppe
      shortDelay,
      CardsDealt(user2.id, List(Card(Asso, Denari)), Direction.Player),     // Sei Coppe, Re Coppe, Asso Denari
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-2121139329),
      CardPlayed(user1.id, Card(Quattro, Coppe)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(-1184832185),
      CardPlayed(user2.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(user2.id),  // 41

      mediumDelay,
      CardsDealt(user2.id, List(Card(Sette, Spade)), Direction.Player),    // Re Coppe, Asso Denari, Sette Spade
      shortDelay,
      CardsDealt(user1.id, List(Card(Cinque, Denari)), Direction.Player),  // Tre Coppe, Fante Coppe, Cinque Denari
      ActionRequested(user2.id, Action.PlayCard),
      willTick(1059467615),
      CardPlayed(user2.id, Card(Sette, Spade)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(769022323),
      CardPlayed(user1.id, Card(Cinque, Denari)),
      mediumDelay,
      TrickCompleted(user2.id),  // 41

      mediumDelay,
      CardsDealt(user2.id, List(Card(Sette, Coppe)), Direction.Player),    // Re Coppe, Asso Denari, Sette Coppe
      shortDelay,
      CardsDealt(user1.id, List(Card(Re, Spade)), Direction.Player),       // Tre Coppe, Fante Coppe, Re Spade
      ActionRequested(user2.id, Action.PlayCard),
      willTick(1344538995),
      CardPlayed(user2.id, Card(Sette, Coppe)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(413423647),
      CardPlayed(user1.id, Card(Re, Spade)),
      mediumDelay,
      TrickCompleted(user2.id),  // 45

      mediumDelay,
      CardsDealt(user2.id, List(Card(Sei, Spade)), Direction.Player),      // Re Coppe, Asso Denari, Sei Spade
      shortDelay,
      CardsDealt(user1.id, List(Card(Quattro, Denari)), Direction.Player), // Tre Coppe, Fante Coppe, Quattro Denari
      ActionRequested(user2.id, Action.PlayCard),
      willTick(1464674929),
      CardPlayed(user2.id, Card(Sei, Spade)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(2031185269),
      CardPlayed(user1.id, Card(Quattro, Denari)),
      mediumDelay,
      TrickCompleted(user2.id),  // 45

      mediumDelay,
      CardsDealt(user2.id, List(Card(Tre, Bastoni)), Direction.Player),    // Re Coppe, Asso Denari, Tre Bastoni
      shortDelay,
      CardsDealt(user1.id, List(Card(Due, Spade)), Direction.Player),      // Tre Coppe, Fante Coppe, Due Spade
      ActionRequested(user2.id, Action.PlayCard),
      willTick(1936713649),
      CardPlayed(user2.id, Card(Tre, Bastoni)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1516142369),
      CardPlayed(user1.id, Card(Fante, Coppe)),
      mediumDelay,
      TrickCompleted(user1.id),  // 47

      mediumDelay,
      CardsDealt(user1.id, List(Card(Cavallo, Coppe)), Direction.Player),  // Tre Coppe, Due Spade, Cavallo Coppe
      shortDelay,
      CardsDealt(user2.id, List(Card(Cinque, Coppe)), Direction.Player),   // Re Coppe, Asso Denari, Cinque Coppe
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1939331415),
      CardPlayed(user1.id, Card(Due, Spade)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(1733750529),
      CardPlayed(user2.id, Card(Asso, Denari)),
      mediumDelay,
      TrickCompleted(user1.id),  // 58

      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1925663179),
      CardPlayed(user1.id, Card(Cavallo, Coppe)),
      ActionRequested(user2.id, Action.PlayCard),
      willTick(2143146509),
      CardPlayed(user2.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(user2.id),  // 52

      ActionRequested(user2.id, Action.PlayCard),
      willTick(251165969),
      CardPlayed(user2.id, Card(Cinque, Coppe)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1806141139),
      CardPlayed(user1.id, Card(Tre, Coppe)),
      mediumDelay,
      TrickCompleted(user1.id),  // 68

      longDelay,
      BriscolaGameCompleted(
        scores = List(
          GameScore(List(user1.id), Nil),
          GameScore(List(user2.id), Nil),
        ),
        matchScores = List(
          MatchScore(List(user1.id), 1),
          MatchScore(List(user2.id), 0)
        )
      )
    ).map(_.toMessage(room1))
  }

  "Irrelevant messages are ignored" ignore {
    val input = fs2.Stream(
      ShuffleDeck(shuffleSeed).toMessage(room1),
      ShuffleDeck(1).toMessage(room1),     // ignored (already shuffled)
      drawCards.toMessage(room1),
      drawCards.toMessage(room1),
      drawCards.toMessage(room1),
      drawCards.toMessage(room1),
      drawCards.toMessage(room1),
      drawCards.toMessage(room1),
      revealTrump.toMessage(room1),
      Continue.toMessage(room1),                                        // ignored, waiting for a player to play
      PlayCard(user2.id, Card(Asso, Spade)).toMessage(room1),         // ignored (not your turn)
      PlayCard(user1.id, Card(Asso, Spade)).toMessage(room1),         // ignored (not your card)
      PlayCard(user1.id, Card(Due, Bastoni)).toMessage(RoomId.newId), // ignored (different room)
    )

    Game.playGame[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(user1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(user2.id, List(Card(Quattro, Spade), Card(Sei, Denari), Card(Re, Denari)), Direction.Player),
      mediumDelay,
      TrumpRevealed(Card(Cinque, Coppe)),
      ActionRequested(user1.id, Action.PlayCard),
      willTick(-1696831491),
    ).map(_.toMessage(room1))
  }

  "Game is aborted if one of the players leave" ignore {
    val input = fs2.Stream[cats.Id, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      drawCards,
      PlayerLeftTable(user1, 1),
      drawCards, // too late, game was aborted
    ).map(_.toMessage(room1))

    Game.playGame[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(user1.id, List(Card(Due, Bastoni)), Direction.Player),
      shortDelay,
      GameAborted
    ).map(_.toMessage(room1))
  }

  "Game continues if another player joins and leaves" ignore {
    val input = fs2.Stream[fs2.Pure, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      PlayerJoinedTable(user3, 2),
      PlayerLeftTable(user3, 2),
      drawCards,
    ).map(_.toMessage(room1))

    Game.playGame[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(user1.id, List(Card(Due, Bastoni)), Direction.Player),
      shortDelay
    ).map(_.toMessage(room1))
  }
