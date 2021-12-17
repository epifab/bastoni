package bastoni.domain.logic
package briscola

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola3Spec extends AnyFreeSpec with Matchers:
  val roomId = RoomId.newId
  val room = Room.cosy(roomId, player1, player2, player3)

  "A game can be played" in {
    val inputStream = Briscola3Spec.input(roomId, player1, player2, player3)
    val expectedOut = Briscola3Spec.output(roomId, player1, player2, player3)
    Game.playMatch[fs2.Pure](room, messageIds)(inputStream).compile.toList shouldBe expectedOut
  }

object Briscola3Spec:

  val drawCard      = Continue
  val revealTrump   = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  def input(roomId: RoomId, player1: Player, player2: Player, player3: Player): fs2.Stream[fs2.Pure, Message] =
    fs2.Stream(
      ShuffleDeck(10),

      drawCard,
      drawCard,
      drawCard,
      drawCard,
      drawCard,
      drawCard,
      drawCard,
      drawCard,
      drawCard,
      revealTrump,

      PlayCard(player1.id, Card(Quattro, Spade)),
      PlayCard(player2.id, Card(Asso, Spade)),
      PlayCard(player3.id, Card(Cinque, Spade)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player2.id, Card(Sei, Denari)),
      PlayCard(player3.id, Card(Sette, Denari)),
      PlayCard(player1.id, Card(Cinque, Coppe)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player3.id, Card(Fante, Bastoni)),
      PlayCard(player1.id, Card(Due, Denari)),
      PlayCard(player2.id, Card(Fante, Spade)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player3.id, Card(Re, Denari)),
      PlayCard(player1.id, Card(Due, Bastoni)),
      PlayCard(player2.id, Card(Tre, Spade)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player1.id, Card(Fante, Coppe)),
      PlayCard(player2.id, Card(Cinque, Bastoni)),
      PlayCard(player3.id, Card(Sei, Coppe)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player2.id, Card(Cavallo, Denari)),
      PlayCard(player3.id, Card(Tre, Denari)),
      PlayCard(player1.id, Card(Sette, Bastoni)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player1.id, Card(Cavallo, Spade)),
      PlayCard(player2.id, Card(Quattro, Bastoni)),
      PlayCard(player3.id, Card(Cavallo, Bastoni)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player3.id, Card(Quattro, Coppe)),
      PlayCard(player1.id, Card(Asso, Coppe)),
      PlayCard(player2.id, Card(Asso, Bastoni)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player2.id, Card(Cinque, Denari)),
      PlayCard(player3.id, Card(Sette, Coppe)),
      PlayCard(player1.id, Card(Re, Spade)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player2.id, Card(Sei, Spade)),
      PlayCard(player3.id, Card(Quattro, Denari)),
      PlayCard(player1.id, Card(Fante, Denari)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      PlayCard(player2.id, Card(Due, Spade)),
      PlayCard(player3.id, Card(Cavallo, Coppe)),
      PlayCard(player1.id, Card(Sei, Bastoni)),
      completeTrick,

      PlayCard(player1.id, Card(Asso, Denari)),
      PlayCard(player2.id, Card(Sette, Spade)),
      PlayCard(player3.id, Card(Re, Bastoni)),
      completeTrick,

      PlayCard(player3.id, Card(Re, Coppe)),
      PlayCard(player1.id, Card(Tre, Bastoni)),
      PlayCard(player2.id, Card(Tre, Coppe)),
      completeTrick,
      completeMatch,
    ).map(_.toMessage(roomId))

  def output(roomId: RoomId, player1: Player, player2: Player, player3: Player): List[Message | Delayed[Message]] =
    List[Event | Command | Delayed[Command]](
      DeckShuffled(10),

      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Sette, Denari), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Quattro, Spade), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Denari), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Re, Denari), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Coppe), Face.Player),     // Due Bastoni, Quattro Spade, Cinque Coppe
      shortDelay,
      CardDealt(player2.id, Card(Asso, Bastoni), Face.Player),     // Asso Spade, Sei Denari, Asso Bastoni
      shortDelay,
      CardDealt(player3.id, Card(Cinque, Spade), Face.Player),     // Sette Denari, Re Denari, Cinque Spade

      mediumDelay,
      TrumpRevealed(Card(Sei, Bastoni)),

      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Quattro, Spade)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Asso, Spade)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 11

      mediumDelay,
      CardDealt(player2.id, Card(Tre, Spade), Face.Player),        // Sei Denari, Asso Bastoni, Tre Spade
      shortDelay,
      CardDealt(player3.id, Card(Tre, Denari), Face.Player),       // Sette Denari, Re Denari, Tre Denari
      shortDelay,
      CardDealt(player1.id, Card(Asso, Coppe), Face.Player),       // Due Bastoni, Cinque Coppe, Asso Coppe
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sei, Denari)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Sette, Denari)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cinque, Coppe)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0

      mediumDelay,
      CardDealt(player3.id, Card(Fante, Bastoni), Face.Player),    // Re Denari, Tre Denari, Fante Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Due, Denari), Face.Player),       // Due Bastoni, Asso Coppe, Due Denari
      shortDelay,
      CardDealt(player2.id, Card(Fante, Spade), Face.Player),      // Asso Bastoni, Tre Spade, Fante Spade
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Fante, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Due, Denari)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Fante, Spade)),
      mediumDelay,
      TrickCompleted(player3.id),  // 4

      mediumDelay,
      CardDealt(player3.id, Card(Re, Bastoni), Face.Player),       // Re Denari, Tre Denari, Re Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Sette, Bastoni), Face.Player),    // Due Bastoni, Asso Coppe, Sette Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Tre, Coppe), Face.Player),        // Asso Bastoni, Tre Spade, Tre Coppe
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Re, Denari)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Tre, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 14

      mediumDelay,
      CardDealt(player1.id, Card(Fante, Coppe), Face.Player),      // Asso Coppe, Sette Bastoni, Fante Coppe
      shortDelay,
      CardDealt(player2.id, Card(Cinque, Bastoni), Face.Player),   // Asso Bastoni, Cinque Bastoni, Tre Coppe
      shortDelay,
      CardDealt(player3.id, Card(Sei, Coppe), Face.Player),        // Tre Denari, Re Bastoni, Sei Coppe
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 2

      mediumDelay,
      CardDealt(player2.id, Card(Cavallo, Denari), Face.Player),   // Asso Bastoni, Tre Coppe, Cavallo Denari
      shortDelay,
      CardDealt(player3.id, Card(Cavallo, Bastoni), Face.Player),  // Tre Denari, Re Bastoni, Cavallo Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Fante, Denari), Face.Player),     // Asso Coppe, Sette Bastoni, Fante Denari
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cavallo, Denari)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Tre, Denari)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 13

      mediumDelay,
      CardDealt(player1.id, Card(Cavallo, Spade), Face.Player),   // Asso Coppe, Fante Denari, Cavallo Spade
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Bastoni), Face.Player), // Asso Bastoni, Tre Coppe, Quattro Bastoni
      shortDelay,
      CardDealt(player3.id, Card(Re, Coppe), Face.Player),        // Re Bastoni, Cavallo Bastoni, Re Coppe
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cavallo, Spade)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Quattro, Bastoni)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Cavallo, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 6

      mediumDelay,
      CardDealt(player3.id, Card(Quattro, Coppe), Face.Player),  // Re Bastoni, Re Coppe, Quattro Coppe
      shortDelay,
      CardDealt(player1.id, Card(Asso, Denari), Face.Player),    // Asso Coppe, Fante Denari, Asso Denari
      shortDelay,
      CardDealt(player2.id, Card(Sette, Spade), Face.Player),    // Asso Bastoni, Tre Coppe, Sette Spade
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Quattro, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Asso, Coppe)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(player2.id),  // 22

      mediumDelay,
      CardDealt(player2.id, Card(Cinque, Denari), Face.Player),  // Tre Coppe, Sette Spade, Cinque Denari
      shortDelay,
      CardDealt(player3.id, Card(Sette, Coppe), Face.Player),    // Re Bastoni, Re Coppe, Sette Coppe
      shortDelay,
      CardDealt(player1.id, Card(Re, Spade), Face.Player),       // Fante Denari, Asso Denari, Re Spade
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cinque, Denari)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Sette, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Re, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 4

      mediumDelay,
      CardDealt(player2.id, Card(Sei, Spade), Face.Player),      // Tre Coppe, Sette Spade, Sei Spade
      shortDelay,
      CardDealt(player3.id, Card(Quattro, Denari), Face.Player), // Re Bastoni, Re Coppe, Quattro Denari
      shortDelay,
      CardDealt(player1.id, Card(Tre, Bastoni), Face.Player),    // Fante Denari, Asso Denari, Tre Bastoni
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sei, Spade)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Quattro, Denari)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Fante, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 0

      mediumDelay,
      CardDealt(player2.id, Card(Due, Spade), Face.Player),      // Tre Coppe, Sette Spade, Due Spade
      shortDelay,
      CardDealt(player3.id, Card(Cavallo, Coppe), Face.Player),  // Re Bastoni, Re Coppe, Cavallo Coppe
      shortDelay,
      CardDealt(player1.id, Card(Sei, Bastoni), Face.Player),    // Asso Denari, Tre Bastoni, Sei Bastoni
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Due, Spade)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Cavallo, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 3

      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Asso, Denari)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sette, Spade)),
      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 15

      ActionRequest(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Re, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Tre, Bastoni)),
      ActionRequest(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Tre, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 24

      longDelay,
      PointsCount(List(player1.id), 54),
      PointsCount(List(player2.id), 41),
      PointsCount(List(player3.id), 25),
      MatchCompleted(List(player1.id))
    ).map(_.toMessage(roomId))
