package bastoni.backend
package briscola

import bastoni.backend
import bastoni.backend.Fixtures.*
import bastoni.backend.briscola.Game
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola2Spec extends AnyFreeSpec with Matchers:

  val roomId = RoomId.newId
  val room = Room(roomId, List(player1, player2))

  val drawCard      = Continue
  val revealTrump   = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  "A game can be played" in {
    val input =
      fs2.Stream(
        ShuffleDeck(10),

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

      ).map(_.toMessage(roomId))

    Game.playMatch[fs2.Pure](room, messageIds)(input).compile.toList shouldBe List[Event | Command | Delayed[Command]](
      DeckShuffled(10),
      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni)),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade)),
      shortDelay,
      CardDealt(player1.id, Card(Sette, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Spade)),
      shortDelay,
      CardDealt(player1.id, Card(Sei, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrumpRevealed(Card(Cinque, Coppe)),

      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Quattro, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 0

      mediumDelay,
      CardDealt(player1.id, Card(Asso, Bastoni)),     // Sette Denari, Sei Denari, Asso Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Cinque, Spade)),     // Asso Spade, Re Denari, Cinque spade
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Sei, Denari)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 4

      mediumDelay,
      CardDealt(player2.id, Card(Sei, Bastoni)),      // Asso Spade, Cinque Spade, Sei Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Tre, Spade)),        // Sette Denari, Asso Bastoni, Tre Spade
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cinque, Spade)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Tre, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 10

      mediumDelay,
      CardDealt(player1.id, Card(Tre, Denari)),       // Sette Denari, Asso Bastoni, Tre Denari
      shortDelay,
      CardDealt(player2.id, Card(Asso, Coppe)),       // Asso Spade, Sei Bastoni, Asso Coppe
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Sette, Denari)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 10

      mediumDelay,
      CardDealt(player1.id, Card(Fante, Bastoni)),    // Asso Bastoni, Tre Denari, Fante Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Due, Denari)),       // Asso Spade, Asso Coppe, Due Denari
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Fante, Bastoni)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Due, Denari)),
      mediumDelay,
      TrickCompleted(player1.id),  // 12

      mediumDelay,
      CardDealt(player1.id, Card(Fante, Spade)),      // Asso Bastoni, Tre Denari, Fante Spade
      shortDelay,
      CardDealt(player2.id, Card(Re, Bastoni)),       // Asso Spade, Asso Coppe, Re Bastoni
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Tre, Denari)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 25

      mediumDelay,
      CardDealt(player2.id, Card(Sette, Bastoni)),    // Asso Spade, Re Bastoni, Sette Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Tre, Coppe)),        // Asso Bastoni, Fante Spade, Tre Coppe
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sette, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 23

      mediumDelay,
      CardDealt(player1.id, Card(Fante, Coppe)),      // Fante Spade, Tre Coppe, Fante Coppe
      shortDelay,
      CardDealt(player2.id, Card(Cinque, Bastoni)),   // Asso Spade, Re Bastoni, Cinque Bastoni
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Fante, Spade)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Asso, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 38

      mediumDelay,
      CardDealt(player2.id, Card(Sei, Coppe)),        // Re Bastoni, Cinque Bastoni, Sei Coppe
      shortDelay,
      CardDealt(player1.id, Card(Cavallo, Denari)),   // Tre Coppe, Fante Coppe, Cavallo Denari
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cavallo, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 41

      mediumDelay,
      CardDealt(player2.id, Card(Cavallo, Bastoni)),  // Re Bastoni, Sei Coppe, Cavallo Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Due, Coppe)),        // Tre Coppe, Fante Coppe, Due Coppe
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Re, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Due, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 27

      mediumDelay,
      CardDealt(player1.id, Card(Fante, Denari)),     // Tre Coppe, Fante Coppe, Fante Denari
      shortDelay,
      CardDealt(player2.id, Card(Cavallo, Spade)),    // Sei Coppe, Cavallo Bastoni, Cavallo Spade
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Fante, Denari)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cavallo, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 32

      mediumDelay,
      CardDealt(player1.id, Card(Quattro, Bastoni)), // Tre Coppe, Fante Coppe, Quattro Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Re, Coppe)),        // Sei Coppe, Cavallo Spade, Re Coppe
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Quattro, Bastoni)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cavallo, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 35

      mediumDelay,
      CardDealt(player1.id, Card(Quattro, Coppe)),   // Tre Coppe, Fante Coppe, Quattro Coppe
      shortDelay,
      CardDealt(player2.id, Card(Asso, Denari)),     // Sei Coppe, Re Coppe, Asso Denari
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Quattro, Coppe)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 41

      mediumDelay,
      CardDealt(player2.id, Card(Sette, Spade)),    // Re Coppe, Asso Denari, Sette Spade
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Denari)),  // Tre Coppe, Fante Coppe, Cinque Denari
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sette, Spade)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cinque, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 41

      mediumDelay,
      CardDealt(player2.id, Card(Sette, Coppe)),    // Re Coppe, Asso Denari, Sette Coppe
      shortDelay,
      CardDealt(player1.id, Card(Re, Spade)),       // Tre Coppe, Fante Coppe, Re Spade
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sette, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Re, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 45

      mediumDelay,
      CardDealt(player2.id, Card(Sei, Spade)),      // Re Coppe, Asso Denari, Sei Spade
      shortDelay,
      CardDealt(player1.id, Card(Quattro, Denari)), // Tre Coppe, Fante Coppe, Quattro Denari
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sei, Spade)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Quattro, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 45

      mediumDelay,
      CardDealt(player2.id, Card(Tre, Bastoni)),    // Re Coppe, Asso Denari, Tre Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Due, Spade)),      // Tre Coppe, Fante Coppe, Due Spade
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Tre, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 47

      mediumDelay,
      CardDealt(player1.id, Card(Cavallo, Coppe)),  // Tre Coppe, Due Spade, Cavallo Coppe
      shortDelay,
      CardDealt(player2.id, Card(Cinque, Coppe)),   // Re Coppe, Asso Denari, Cinque Coppe
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Due, Spade)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Asso, Denari)),
      mediumDelay,
      TrickCompleted(player1.id),  // 58

      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cavallo, Coppe)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 52

      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cinque, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Tre, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 68

      longDelay,
      PointsCount(List(player1.id), 68),
      PointsCount(List(player2.id), 52),
      MatchCompleted(List(player1.id))
    ).map(_.toMessage(roomId))
  }

  "Irrelevant messages are ignored" in {
    val input = fs2.Stream(
      ShuffleDeck(10).toMessage(room.id),
      ShuffleDeck(1).toMessage(room.id),     // ignored (already shuffled)
      drawCard.toMessage(room.id),
      drawCard.toMessage(room.id),
      drawCard.toMessage(room.id),
      drawCard.toMessage(room.id),
      drawCard.toMessage(room.id),
      drawCard.toMessage(room.id),
      revealTrump.toMessage(room.id),
      Continue.toMessage(room.id),                                      // ignored, waiting for a player to play
      PlayCard(player2.id, Card(Asso, Spade)).toMessage(room.id),       // ignored (not your turn)
      PlayCard(player1.id, Card(Asso, Spade)).toMessage(room.id),       // ignored (not your card)
      PlayCard(player1.id, Card(Due, Bastoni)).toMessage(RoomId.newId), // ignored (different room)
    )

    Game.playMatch[fs2.Pure](room, messageIds)(input).compile.toList shouldBe List[Event | Command | Delayed[Command]](
      DeckShuffled(10),
      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni)),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade)),
      shortDelay,
      CardDealt(player1.id, Card(Sette, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Spade)),
      shortDelay,
      CardDealt(player1.id, Card(Sei, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrumpRevealed(Card(Cinque, Coppe)),
      ActionRequest(player1.id, Action.PlayCard)
    ).map(_.toMessage(room.id))
  }

  "Game is aborted if a player leaves" in {
    val input = fs2.Stream[fs2.Pure, Command | Event](
      ShuffleDeck(10),
      drawCard,
      PlayerLeft(player1, Room(room.id, List(player2))),
      drawCard, // too late, game was aborted
    ).map(_.toMessage(room.id))

    Game.playMatch[fs2.Pure](room, messageIds)(input).compile.toList shouldBe List[Event | Command | Delayed[Command]](
      DeckShuffled(10),
      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni)),
      shortDelay,
      MatchAborted
    ).map(_.toMessage(room.id))
  }
