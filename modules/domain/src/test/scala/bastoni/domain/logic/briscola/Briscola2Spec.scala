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

  val players = List(player1, player2)

  "A game can be played" in {
    val input =
      fs2.Stream(
        ShuffleDeck(shuffleSeed),

        drawCard,
        drawCard,
        drawCard,
        drawCard,
        drawCard,
        drawCard,
        revealTrump,

        PlayCard(player1.id, Card(Due, Bastoni)),
        PlayCard(player2.id, Card(Quattro, Spade)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Sei, Denari)),
        PlayCard(player2.id, Card(Re, Denari)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Cinque, Spade)),
        PlayCard(player1.id, Card(Tre, Spade)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Sette, Denari)),
        PlayCard(player2.id, Card(Sei, Bastoni)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Fante, Bastoni)),
        PlayCard(player2.id, Card(Due, Denari)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Tre, Denari)),
        PlayCard(player2.id, Card(Asso, Coppe)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Sette, Bastoni)),
        PlayCard(player1.id, Card(Asso, Bastoni)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Fante, Spade)),
        PlayCard(player2.id, Card(Asso, Spade)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Cinque, Bastoni)),
        PlayCard(player1.id, Card(Cavallo, Denari)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Re, Bastoni)),
        PlayCard(player1.id, Card(Due, Coppe)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Fante, Denari)),
        PlayCard(player2.id, Card(Cavallo, Bastoni)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Quattro, Bastoni)),
        PlayCard(player2.id, Card(Cavallo, Spade)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Quattro, Coppe)),
        PlayCard(player2.id, Card(Sei, Coppe)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Sette, Spade)),
        PlayCard(player1.id, Card(Cinque, Denari)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Sette, Coppe)),
        PlayCard(player1.id, Card(Re, Spade)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Sei, Spade)),
        PlayCard(player1.id, Card(Quattro, Denari)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player2.id, Card(Tre, Bastoni)),
        PlayCard(player1.id, Card(Fante, Coppe)),
        completeTrick,

        drawCard,
        drawCard,
        PlayCard(player1.id, Card(Due, Spade)),
        PlayCard(player2.id, Card(Asso, Denari)),
        completeTrick,

        PlayCard(player1.id, Card(Cavallo, Coppe)),
        PlayCard(player2.id, Card(Re, Coppe)),
        completeTrick,

        PlayCard(player2.id, Card(Cinque, Coppe)),
        PlayCard(player1.id, Card(Tre, Coppe)),
        completeTrick,
        completeMatch,

      ).map(_.toMessage(room1))

    Game.playMatch[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(player1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(player2.id, List(Card(Quattro, Spade), Card(Sei, Denari), Card(Re, Denari)), Direction.Player),
      mediumDelay,
      TrumpRevealed(Card(Cinque, Coppe)),

      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1696831491),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1227817415),
      CardPlayed(player2.id, Card(Quattro, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 0

      mediumDelay,
      CardsDealt(player1.id, List(Card(Asso, Bastoni)), Direction.Player),     // Sette Denari, Sei Denari, Asso Bastoni
      shortDelay,
      CardsDealt(player2.id, List(Card(Cinque, Spade)), Direction.Player),     // Asso Spade, Re Denari, Cinque spade
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1557019883),
      CardPlayed(player1.id, Card(Sei, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1519423663),
      CardPlayed(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 4

      mediumDelay,
      CardsDealt(player2.id, List(Card(Sei, Bastoni)), Direction.Player),      // Asso Spade, Cinque Spade, Sei Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Tre, Spade)), Direction.Player),        // Sette Denari, Asso Bastoni, Tre Spade
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-485809279),
      CardPlayed(player2.id, Card(Cinque, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1610431497),
      CardPlayed(player1.id, Card(Tre, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 10

      mediumDelay,
      CardsDealt(player1.id, List(Card(Tre, Denari)), Direction.Player),       // Sette Denari, Asso Bastoni, Tre Denari
      shortDelay,
      CardsDealt(player2.id, List(Card(Asso, Coppe)), Direction.Player),       // Asso Spade, Sei Bastoni, Asso Coppe
      ActionRequested(player1.id, Action.PlayCard),
      willTick(2118766723),
      CardPlayed(player1.id, Card(Sette, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1786131071),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 10

      mediumDelay,
      CardsDealt(player1.id, List(Card(Fante, Bastoni)), Direction.Player),    // Asso Bastoni, Tre Denari, Fante Bastoni
      shortDelay,
      CardsDealt(player2.id, List(Card(Due, Denari)), Direction.Player),       // Asso Spade, Asso Coppe, Due Denari
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-615401723),
      CardPlayed(player1.id, Card(Fante, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(150476751),
      CardPlayed(player2.id, Card(Due, Denari)),
      mediumDelay,
      TrickCompleted(player1.id),  // 12

      mediumDelay,
      CardsDealt(player1.id, List(Card(Fante, Spade)), Direction.Player),      // Asso Bastoni, Tre Denari, Fante Spade
      shortDelay,
      CardsDealt(player2.id, List(Card(Re, Bastoni)), Direction.Player),       // Asso Spade, Asso Coppe, Re Bastoni
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1415975155),
      CardPlayed(player1.id, Card(Tre, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-722753605),
      CardPlayed(player2.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 25

      mediumDelay,
      CardsDealt(player2.id, List(Card(Sette, Bastoni)), Direction.Player),    // Asso Spade, Re Bastoni, Sette Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Tre, Coppe)), Direction.Player),        // Asso Bastoni, Fante Spade, Tre Coppe
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-888680513),
      CardPlayed(player2.id, Card(Sette, Bastoni)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1750257595),
      CardPlayed(player1.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 23

      mediumDelay,
      CardsDealt(player1.id, List(Card(Fante, Coppe)), Direction.Player),      // Fante Spade, Tre Coppe, Fante Coppe
      shortDelay,
      CardsDealt(player2.id, List(Card(Cinque, Bastoni)), Direction.Player),   // Asso Spade, Re Bastoni, Cinque Bastoni
      ActionRequested(player1.id, Action.PlayCard),
      willTick(1086954241),
      CardPlayed(player1.id, Card(Fante, Spade)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1909131905),
      CardPlayed(player2.id, Card(Asso, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 38

      mediumDelay,
      CardsDealt(player2.id, List(Card(Sei, Coppe)), Direction.Player),        // Re Bastoni, Cinque Bastoni, Sei Coppe
      shortDelay,
      CardsDealt(player1.id, List(Card(Cavallo, Denari)), Direction.Player),   // Tre Coppe, Fante Coppe, Cavallo Denari
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1984889977),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-824350673),
      CardPlayed(player1.id, Card(Cavallo, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 41

      mediumDelay,
      CardsDealt(player2.id, List(Card(Cavallo, Bastoni)), Direction.Player),  // Re Bastoni, Sei Coppe, Cavallo Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Due, Coppe)), Direction.Player),        // Tre Coppe, Fante Coppe, Due Coppe
      ActionRequested(player2.id, Action.PlayCard),
      willTick(69503093),
      CardPlayed(player2.id, Card(Re, Bastoni)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(32728529),
      CardPlayed(player1.id, Card(Due, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 27

      mediumDelay,
      CardsDealt(player1.id, List(Card(Fante, Denari)), Direction.Player),     // Tre Coppe, Fante Coppe, Fante Denari
      shortDelay,
      CardsDealt(player2.id, List(Card(Cavallo, Spade)), Direction.Player),    // Sei Coppe, Cavallo Bastoni, Cavallo Spade
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-88664045),
      CardPlayed(player1.id, Card(Fante, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(300625131),
      CardPlayed(player2.id, Card(Cavallo, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 32

      mediumDelay,
      CardsDealt(player1.id, List(Card(Quattro, Bastoni)), Direction.Player), // Tre Coppe, Fante Coppe, Quattro Bastoni
      shortDelay,
      CardsDealt(player2.id, List(Card(Re, Coppe)), Direction.Player),        // Sei Coppe, Cavallo Spade, Re Coppe
      ActionRequested(player1.id, Action.PlayCard),
      willTick(280019095),
      CardPlayed(player1.id, Card(Quattro, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(276103007),
      CardPlayed(player2.id, Card(Cavallo, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 35

      mediumDelay,
      CardsDealt(player1.id, List(Card(Quattro, Coppe)), Direction.Player),   // Tre Coppe, Fante Coppe, Quattro Coppe
      shortDelay,
      CardsDealt(player2.id, List(Card(Asso, Denari)), Direction.Player),     // Sei Coppe, Re Coppe, Asso Denari
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-2121139329),
      CardPlayed(player1.id, Card(Quattro, Coppe)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1184832185),
      CardPlayed(player2.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 41

      mediumDelay,
      CardsDealt(player2.id, List(Card(Sette, Spade)), Direction.Player),    // Re Coppe, Asso Denari, Sette Spade
      shortDelay,
      CardsDealt(player1.id, List(Card(Cinque, Denari)), Direction.Player),  // Tre Coppe, Fante Coppe, Cinque Denari
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1059467615),
      CardPlayed(player2.id, Card(Sette, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(769022323),
      CardPlayed(player1.id, Card(Cinque, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 41

      mediumDelay,
      CardsDealt(player2.id, List(Card(Sette, Coppe)), Direction.Player),    // Re Coppe, Asso Denari, Sette Coppe
      shortDelay,
      CardsDealt(player1.id, List(Card(Re, Spade)), Direction.Player),       // Tre Coppe, Fante Coppe, Re Spade
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1344538995),
      CardPlayed(player2.id, Card(Sette, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(413423647),
      CardPlayed(player1.id, Card(Re, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 45

      mediumDelay,
      CardsDealt(player2.id, List(Card(Sei, Spade)), Direction.Player),      // Re Coppe, Asso Denari, Sei Spade
      shortDelay,
      CardsDealt(player1.id, List(Card(Quattro, Denari)), Direction.Player), // Tre Coppe, Fante Coppe, Quattro Denari
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1464674929),
      CardPlayed(player2.id, Card(Sei, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(2031185269),
      CardPlayed(player1.id, Card(Quattro, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 45

      mediumDelay,
      CardsDealt(player2.id, List(Card(Tre, Bastoni)), Direction.Player),    // Re Coppe, Asso Denari, Tre Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Due, Spade)), Direction.Player),      // Tre Coppe, Fante Coppe, Due Spade
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1936713649),
      CardPlayed(player2.id, Card(Tre, Bastoni)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1516142369),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 47

      mediumDelay,
      CardsDealt(player1.id, List(Card(Cavallo, Coppe)), Direction.Player),  // Tre Coppe, Due Spade, Cavallo Coppe
      shortDelay,
      CardsDealt(player2.id, List(Card(Cinque, Coppe)), Direction.Player),   // Re Coppe, Asso Denari, Cinque Coppe
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1939331415),
      CardPlayed(player1.id, Card(Due, Spade)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1733750529),
      CardPlayed(player2.id, Card(Asso, Denari)),
      mediumDelay,
      TrickCompleted(player1.id),  // 58

      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1925663179),
      CardPlayed(player1.id, Card(Cavallo, Coppe)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(2143146509),
      CardPlayed(player2.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 52

      ActionRequested(player2.id, Action.PlayCard),
      willTick(251165969),
      CardPlayed(player2.id, Card(Cinque, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1806141139),
      CardPlayed(player1.id, Card(Tre, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 68

      longDelay,
      MatchCompleted(
        winnerIds = List(player1.id),
        matchPoints = List(
          PointsCount(List(player1.id), 68),
          PointsCount(List(player2.id), 52),
        ),
        gamePoints = List(
          PointsCount(List(player1.id), 1),
          PointsCount(List(player2.id), 0)
        )
      )
    ).map(_.toMessage(room1))
  }

  "Irrelevant messages are ignored" in {
    val input = fs2.Stream(
      ShuffleDeck(shuffleSeed).toMessage(room1),
      ShuffleDeck(1).toMessage(room1),     // ignored (already shuffled)
      drawCard.toMessage(room1),
      drawCard.toMessage(room1),
      drawCard.toMessage(room1),
      drawCard.toMessage(room1),
      drawCard.toMessage(room1),
      drawCard.toMessage(room1),
      revealTrump.toMessage(room1),
      Continue.toMessage(room1),                                        // ignored, waiting for a player to play
      PlayCard(player2.id, Card(Asso, Spade)).toMessage(room1),         // ignored (not your turn)
      PlayCard(player1.id, Card(Asso, Spade)).toMessage(room1),         // ignored (not your card)
      PlayCard(player1.id, Card(Due, Bastoni)).toMessage(RoomId.newId), // ignored (different room)
    )

    Game.playMatch[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(player1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(player2.id, List(Card(Quattro, Spade), Card(Sei, Denari), Card(Re, Denari)), Direction.Player),
      mediumDelay,
      TrumpRevealed(Card(Cinque, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1696831491),
    ).map(_.toMessage(room1))
  }

  "Game is aborted if one of the players leave" in {
    val input = fs2.Stream[cats.Id, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      drawCard,
      PlayerLeftTable(player1, 1),
      drawCard, // too late, game was aborted
    ).map(_.toMessage(room1))

    Game.playMatch[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(player1.id, List(Card(Due, Bastoni)), Direction.Player),
      shortDelay,
      MatchAborted
    ).map(_.toMessage(room1))
  }

  "Game continues if another player joins and leaves" in {
    val input = fs2.Stream[fs2.Pure, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      PlayerJoinedTable(player3, 2),
      PlayerLeftTable(player3, 2),
      drawCard,
    ).map(_.toMessage(room1))

    Game.playMatch[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(player1.id, List(Card(Due, Bastoni)), Direction.Player),
      shortDelay
    ).map(_.toMessage(room1))
  }
