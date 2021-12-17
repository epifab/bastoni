package bastoni.domain.logic
package briscola

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import cats.catsInstancesForId
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola4Spec extends AnyFreeSpec with Matchers:

  val roomId = RoomId.newId
  val players = List(player1, player2, player3, player4)

  "A game can be played" in {
    val inputStream = Briscola4Spec.input(roomId, player1, player2, player3, player4)
    val expectedOut = Briscola4Spec.output(roomId, player1, player2, player3, player4)
    Game.playMatch[cats.Id](roomId, players, messageId)(inputStream).compile.toList shouldBe expectedOut
  }

object Briscola4Spec:
  val drawCard      = Continue
  val revealTrump   = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  def input(roomId: RoomId, player1: Player, player2: Player, player3: Player, player4: Player) =
    fs2.Stream(
      ShuffleDeck(shuffleSeed),

      drawCard,
      drawCard,
      drawCard,
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

      PlayCard(player1.id, Card(Due, Bastoni)),
      PlayCard(player2.id, Card(Sei, Bastoni)),
      PlayCard(player3.id, Card(Sette, Denari)),
      PlayCard(player4.id, Card(Asso, Bastoni)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      drawCard,
      PlayCard(player4.id, Card(Fante, Bastoni)),
      PlayCard(player1.id, Card(Due, Denari)),
      PlayCard(player2.id, Card(Fante, Spade)),
      PlayCard(player3.id, Card(Re, Bastoni)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      drawCard,
      PlayCard(player3.id, Card(Sette, Bastoni)),
      PlayCard(player4.id, Card(Quattro, Spade)),
      PlayCard(player1.id, Card(Sei, Denari)),
      PlayCard(player2.id, Card(Cinque, Bastoni)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      drawCard,
      PlayCard(player3.id, Card(Sei, Coppe)),
      PlayCard(player4.id, Card(Cavallo, Denari)),
      PlayCard(player1.id, Card(Cavallo, Bastoni)),
      PlayCard(player2.id, Card(Re, Denari)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      drawCard,
      PlayCard(player3.id, Card(Fante, Denari)),
      PlayCard(player4.id, Card(Tre, Denari)),
      PlayCard(player1.id, Card(Quattro, Bastoni)),
      PlayCard(player2.id, Card(Re, Coppe)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      drawCard,
      PlayCard(player2.id, Card(Due, Coppe)),
      PlayCard(player3.id, Card(Cinque, Coppe)),
      PlayCard(player4.id, Card(Sette, Spade)),
      PlayCard(player1.id, Card(Cinque, Spade)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      drawCard,
      PlayCard(player3.id, Card(Sette, Coppe)),
      PlayCard(player4.id, Card(Cavallo, Spade)),
      PlayCard(player1.id, Card(Sei, Spade)),
      PlayCard(player2.id, Card(Quattro, Denari)),
      completeTrick,

      drawCard,
      drawCard,
      drawCard,
      drawCard,

      PlayCard(player3.id, Card(Tre, Bastoni)),
      PlayCard(player4.id, Card(Re, Spade)),
      PlayCard(player1.id, Card(Cavallo, Coppe)),
      PlayCard(player2.id, Card(Quattro, Coppe)),
      completeTrick,

      PlayCard(player1.id, Card(Cinque, Denari)),
      PlayCard(player2.id, Card(Asso, Spade)),
      PlayCard(player3.id, Card(Asso, Denari)),
      PlayCard(player4.id, Card(Due, Spade)),
      completeTrick,

      PlayCard(player3.id, Card(Tre, Spade)),
      PlayCard(player4.id, Card(Tre, Coppe)),
      PlayCard(player1.id, Card(Fante, Coppe)),
      PlayCard(player2.id, Card(Asso, Coppe)),
      completeTrick,
      completeMatch,

    ).map(_.toMessage(roomId))

  def output(roomId: RoomId, player1: Player, player2: Player, player3: Player, player4: Player) =
    List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,

      CardDealt(player1.id, Card(Due, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Sette, Denari), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Quattro, Spade), Face.Player),

      shortDelay,
      CardDealt(player1.id, Card(Sei, Denari), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Re, Denari), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Cinque, Coppe), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Asso, Bastoni), Face.Player),

      shortDelay,
      CardDealt(player1.id, Card(Cinque, Spade), Face.Player), // Due Bastoni, Sei Denari, Cinque Spade
      shortDelay,
      CardDealt(player2.id, Card(Sei, Bastoni), Face.Player),  // Asso Spade, Re Denari, Sei Bastoni
      shortDelay,
      CardDealt(player3.id, Card(Tre, Spade), Face.Player),    // Sette Denari, Cinque Coppe, Tre Spade
      shortDelay,
      CardDealt(player4.id, Card(Tre, Denari), Face.Player),   // Quattro Spade, Asso Bastoni, Tre Denari

      mediumDelay,
      TrumpRevealed(Card(Asso, Coppe)),

      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Sette, Denari)),
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(player4.id),  // 11

      mediumDelay,
      CardDealt(player4.id, Card(Fante, Bastoni), Face.Player),  // Quattro Spade, Tre Denari, Fante Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Due, Denari), Face.Player),     // Sei Denari, Cinque Spade, Due Denari
      shortDelay,
      CardDealt(player2.id, Card(Fante, Spade), Face.Player),    // Asso Spade, Re Denari, Fante Spade
      shortDelay,
      CardDealt(player3.id, Card(Re, Bastoni), Face.Player),     // Cinque Coppe, Tre Spade, Re Bastoni
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Fante, Bastoni)),
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Due, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Fante, Spade)),
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 8

      mediumDelay,
      CardDealt(player3.id, Card(Sette, Bastoni), Face.Player),  // Cinque Coppe, Tre Spade, Sette Bastoni
      shortDelay,
      CardDealt(player4.id, Card(Tre, Coppe), Face.Player),      // Quattro Spade, Tre Denari, Tre Coppe
      shortDelay,
      CardDealt(player1.id, Card(Fante, Coppe), Face.Player),    // Sei Denari, Cinque Spade, Fante Coppe
      shortDelay,
      CardDealt(player2.id, Card(Cinque, Bastoni), Face.Player), // Asso Spade, Re Denari, Cinque Bastoni
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Sette, Bastoni)),
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Quattro, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Sei, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0 + 8 = 8

      mediumDelay,
      CardDealt(player3.id, Card(Sei, Coppe), Face.Player),        // Cinque Coppe, Tre Spade, Sei Coppe
      shortDelay,
      CardDealt(player4.id, Card(Cavallo, Denari), Face.Player),   // Tre Denari, Tre Coppe, Cavallo Denari
      shortDelay,
      CardDealt(player1.id, Card(Cavallo, Bastoni), Face.Player),  // Cinque Spade, Fante Coppe, Cavallo Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Due, Coppe), Face.Player),        // Asso Spade, Re Denari, Due Coppe
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Sei, Coppe)),
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Cavallo, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cavallo, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(player3.id),  // 10 + 8 = 18

      mediumDelay,
      CardDealt(player3.id, Card(Fante, Denari), Face.Player),     // Cinque Coppe, Tre Spade, Fante Denari
      shortDelay,
      CardDealt(player4.id, Card(Cavallo, Spade), Face.Player),    // Tre Denari, Tre Coppe, Cavallo Spade
      shortDelay,
      CardDealt(player1.id, Card(Quattro, Bastoni), Face.Player),  // Cinque Spade, Fante Coppe, Quattro Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Re, Coppe), Face.Player),         // Asso Spade, Due Coppe, Re Coppe
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Fante, Denari)),
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Tre, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Quattro, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 16 + 11 = 27

      mediumDelay,
      CardDealt(player2.id, Card(Quattro, Coppe), Face.Player),  // Asso Spade, Due Coppe, Quattro Coppe
      shortDelay,
      CardDealt(player3.id, Card(Asso, Denari), Face.Player),    // Cinque Coppe, Tre Spade, Asso Denari
      shortDelay,
      CardDealt(player4.id, Card(Sette, Spade), Face.Player),    // Tre Coppe, Cavallo Spade, Sette Spade
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Denari), Face.Player),  // Cinque Spade, Fante Coppe, Cinque Denari
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Due, Coppe)),
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Cinque, Coppe)),
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Sette, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0 + 18 = 18

      mediumDelay,
      CardDealt(player3.id, Card(Sette, Coppe), Face.Player),    // Tre Spade, Asso Denari, Sette Coppe
      shortDelay,
      CardDealt(player4.id, Card(Re, Spade), Face.Player),       // Tre Coppe, Cavallo Spade, Re Spade
      shortDelay,
      CardDealt(player1.id, Card(Sei, Spade), Face.Player),      // Fante Coppe, Cinque Denari, Sei Spade
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Denari), Face.Player), // Asso Spade, Quattro Coppe, Quattro Denari
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Sette, Coppe)),
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Cavallo, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Sei, Spade)),
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Quattro, Denari)),
      mediumDelay,
      TrickCompleted(player3.id),  // 3 + 18 = 21

      mediumDelay,
      CardDealt(player3.id, Card(Tre, Bastoni), Face.Player),      // Tre Spade, Asso Denari, Tre Bastoni
      shortDelay,
      CardDealt(player4.id, Card(Due, Spade), Face.Player),        // Tre Coppe, Re Spade, Due Spade
      shortDelay,
      CardDealt(player1.id, Card(Cavallo, Coppe), Face.Player),    // Fante Coppe, Cinque Denari, Cavallo Coppe
      shortDelay,
      CardDealt(player2.id, Card(Asso, Coppe), Face.Player),       // Asso Spade, Quattro Coppe, Asso Coppe

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Tre, Bastoni)),      // Tre Spade, Asso Denari, ***
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Re, Spade)),         // Tre Coppe, ***, Due Spade
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cavallo, Coppe)),    // Fante Coppe, Cinque Denari, ***
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Quattro, Coppe)),    // Asso Spade, ***, Asso Coppe
      mediumDelay,
      TrickCompleted(player1.id),  // 17 + 21 = 38

      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Cinque, Denari)),    // Fante Coppe, ***, ***
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Asso, Spade)),       // ***, ***, Asso Coppe
      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Asso, Denari)),      // Tre Spade, ***, ***
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Due, Spade)),        // Tre Coppe, ***, ***
      mediumDelay,
      TrickCompleted(player3.id),  // 22 + 38 = 60

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Tre, Spade)),
      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Tre, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      ActionRequested(player2.id, Action.PlayCard),
      CardPlayed(player2.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 33 + 27 = 60

      longDelay,
      MatchCompleted(
        winnerIds = Nil,
        matchPoints = List(
          PointsCount(List(player2.id, player4.id), 60),
          PointsCount(List(player3.id, player1.id), 60)
        ),
        gamePoints = List(
          PointsCount(List(player2.id, player4.id), 0),
          PointsCount(List(player3.id, player1.id), 0)
        )
      )
    ).map(_.toMessage(roomId))
