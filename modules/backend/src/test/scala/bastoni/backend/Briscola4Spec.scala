package bastoni.backend

import bastoni.backend.briscola.Game
import bastoni.domain.model.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola4Spec extends AnyFreeSpec with Matchers:
  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")
  val player4 = Player(PlayerId.newId, "Giuda")

  val roomId = RoomId.newId
  val room = Room(roomId, List(player1, player2, player3, player4))

  "A game can be played" in {
    val inputStream = Briscola4Spec.input(roomId, player1, player2, player3, player4)
    val expectedOut = Briscola4Spec.output(roomId, player1, player2, player3, player4)
    Game.playMatch[fs2.Pure](room)(inputStream).compile.toList shouldBe expectedOut
  }

object Briscola4Spec:
  val drawCard      = Continue
  val revealTrump   = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  val shortDelay = DelayedCommand(Continue, Delay.Short)
  val mediumDelay = DelayedCommand(Continue, Delay.Medium)
  val longDelay = DelayedCommand(Continue, Delay.Long)

  def input(roomId: RoomId, player1: Player, player2: Player, player3: Player, player4: Player) =
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

    ).map(Message(roomId, _))

  def output(roomId: RoomId, player1: Player, player2: Player, player3: Player, player4: Player) =
    List[Event | Command | DelayedCommand](
      DeckShuffled(10),
      mediumDelay,

      CardDealt(player1.id, Card(Due, Bastoni)),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade)),
      shortDelay,
      CardDealt(player3.id, Card(Sette, Denari)),
      shortDelay,
      CardDealt(player4.id, Card(Quattro, Spade)),

      shortDelay,
      CardDealt(player1.id, Card(Sei, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Re, Denari)),
      shortDelay,
      CardDealt(player3.id, Card(Cinque, Coppe)),
      shortDelay,
      CardDealt(player4.id, Card(Asso, Bastoni)),

      shortDelay,
      CardDealt(player1.id, Card(Cinque, Spade)), // Due Bastoni, Sei Denari, Cinque Spade
      shortDelay,
      CardDealt(player2.id, Card(Sei, Bastoni)),  // Asso Spade, Re Denari, Sei Bastoni
      shortDelay,
      CardDealt(player3.id, Card(Tre, Spade)),    // Sette Denari, Cinque Coppe, Tre Spade
      shortDelay,
      CardDealt(player4.id, Card(Tre, Denari)),   // Quattro Spade, Asso Bastoni, Tre Denari

      mediumDelay,
      TrumpRevealed(Card(Asso, Coppe)),

      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Sette, Denari)),
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(player4.id),  // 11

      mediumDelay,
      CardDealt(player4.id, Card(Fante, Bastoni)),  // Quattro Spade, Tre Denari, Fante Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Due, Denari)),     // Sei Denari, Cinque Spade, Due Denari
      shortDelay,
      CardDealt(player2.id, Card(Fante, Spade)),    // Asso Spade, Re Denari, Fante Spade
      shortDelay,
      CardDealt(player3.id, Card(Re, Bastoni)),     // Cinque Coppe, Tre Spade, Re Bastoni
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Fante, Bastoni)),
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Due, Denari)),
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Fante, Spade)),
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 8

      mediumDelay,
      CardDealt(player3.id, Card(Sette, Bastoni)),  // Cinque Coppe, Tre Spade, Sette Bastoni
      shortDelay,
      CardDealt(player4.id, Card(Tre, Coppe)),      // Quattro Spade, Tre Denari, Tre Coppe
      shortDelay,
      CardDealt(player1.id, Card(Fante, Coppe)),    // Sei Denari, Cinque Spade, Fante Coppe
      shortDelay,
      CardDealt(player2.id, Card(Cinque, Bastoni)), // Asso Spade, Re Denari, Cinque Bastoni
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Sette, Bastoni)),
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Quattro, Spade)),
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Sei, Denari)),
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0 + 8 = 8

      mediumDelay,
      CardDealt(player3.id, Card(Sei, Coppe)),        // Cinque Coppe, Tre Spade, Sei Coppe
      shortDelay,
      CardDealt(player4.id, Card(Cavallo, Denari)),   // Tre Denari, Tre Coppe, Cavallo Denari
      shortDelay,
      CardDealt(player1.id, Card(Cavallo, Bastoni)),  // Cinque Spade, Fante Coppe, Cavallo Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Due, Coppe)),        // Asso Spade, Re Denari, Due Coppe
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Sei, Coppe)),
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Cavallo, Denari)),
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Cavallo, Bastoni)),
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(player3.id),  // 10 + 8 = 18

      mediumDelay,
      CardDealt(player3.id, Card(Fante, Denari)),     // Cinque Coppe, Tre Spade, Fante Denari
      shortDelay,
      CardDealt(player4.id, Card(Cavallo, Spade)),    // Tre Denari, Tre Coppe, Cavallo Spade
      shortDelay,
      CardDealt(player1.id, Card(Quattro, Bastoni)),  // Cinque Spade, Fante Coppe, Quattro Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Re, Coppe)),         // Asso Spade, Due Coppe, Re Coppe
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Fante, Denari)),
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Tre, Denari)),
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Quattro, Bastoni)),
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 16 + 11 = 27

      mediumDelay,
      CardDealt(player2.id, Card(Quattro, Coppe)),  // Asso Spade, Due Coppe, Quattro Coppe
      shortDelay,
      CardDealt(player3.id, Card(Asso, Denari)),    // Cinque Coppe, Tre Spade, Asso Denari
      shortDelay,
      CardDealt(player4.id, Card(Sette, Spade)),    // Tre Coppe, Cavallo Spade, Sette Spade
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Denari)),  // Cinque Spade, Fante Coppe, Cinque Denari
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Due, Coppe)),
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Cinque, Coppe)),
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Sette, Spade)),
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0 + 18 = 18

      mediumDelay,
      CardDealt(player3.id, Card(Sette, Coppe)),    // Tre Spade, Asso Denari, Sette Coppe
      shortDelay,
      CardDealt(player4.id, Card(Re, Spade)),       // Tre Coppe, Cavallo Spade, Re Spade
      shortDelay,
      CardDealt(player1.id, Card(Sei, Spade)),      // Fante Coppe, Cinque Denari, Sei Spade
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Denari)), // Asso Spade, Quattro Coppe, Quattro Denari
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Sette, Coppe)),
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Cavallo, Spade)),
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Sei, Spade)),
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Quattro, Denari)),
      mediumDelay,
      TrickCompleted(player3.id),  // 3 + 18 = 21

      mediumDelay,
      CardDealt(player3.id, Card(Tre, Bastoni)),      // Tre Spade, Asso Denari, Tre Bastoni
      shortDelay,
      CardDealt(player4.id, Card(Due, Spade)),        // Tre Coppe, Re Spade, Due Spade
      shortDelay,
      CardDealt(player1.id, Card(Cavallo, Coppe)),    // Fante Coppe, Cinque Denari, Cavallo Coppe
      shortDelay,
      CardDealt(player2.id, Card(Asso, Coppe)),       // Asso Spade, Quattro Coppe, Asso Coppe

      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Tre, Bastoni)),      // Tre Spade, Asso Denari, ***
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Re, Spade)),         // Tre Coppe, ***, Due Spade
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Cavallo, Coppe)),    // Fante Coppe, Cinque Denari, ***
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Quattro, Coppe)),    // Asso Spade, ***, Asso Coppe
      mediumDelay,
      TrickCompleted(player1.id),  // 17 + 21 = 38

      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Cinque, Denari)),    // Fante Coppe, ***, ***
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Asso, Spade)),       // ***, ***, Asso Coppe
      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Asso, Denari)),      // Tre Spade, ***, ***
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Due, Spade)),        // Tre Coppe, ***, ***
      mediumDelay,
      TrickCompleted(player3.id),  // 22 + 38 = 60

      ActionRequest(player3.id),
      CardPlayed(player3.id, Card(Tre, Spade)),
      ActionRequest(player4.id),
      CardPlayed(player4.id, Card(Tre, Coppe)),
      ActionRequest(player1.id),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      ActionRequest(player2.id),
      CardPlayed(player2.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 33 + 27 = 60

      longDelay,
      PointsCount(List(player2.id, player4.id), 60),
      PointsCount(List(player3.id, player1.id), 60),
      MatchDraw
    ).map(_.toMessage(roomId))
