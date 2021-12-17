package bastoni.backend

import bastoni.domain.*
import bastoni.domain.Rank.{Sei, Sette, Tre, *}
import bastoni.domain.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola4Spec extends AnyFreeSpec with Matchers:
  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")
  val player4 = Player(PlayerId.newId, "Giuda")

  val roomId = RoomId.newId
  val room = Room(roomId, List(player1, player2, player3, player4))

  val drawCard      = Continue
  val revealTrump   = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  "A game can be played" in {
    val input = fs2.Stream(
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

    ).map(Message(room.id, _))

    Briscola.playMatch[fs2.Pure](room)(input).map(_.message).compile.toList shouldBe List(
      DeckShuffled(10),

      CardDealt(player1.id, Card(Due, Bastoni)),
      CardDealt(player2.id, Card(Asso, Spade)),
      CardDealt(player3.id, Card(Sette, Denari)),
      CardDealt(player4.id, Card(Quattro, Spade)),

      CardDealt(player1.id, Card(Sei, Denari)),
      CardDealt(player2.id, Card(Re, Denari)),
      CardDealt(player3.id, Card(Cinque, Coppe)),
      CardDealt(player4.id, Card(Asso, Bastoni)),

      CardDealt(player1.id, Card(Cinque, Spade)), // Due Bastoni, Sei Denari, Cinque Spade
      CardDealt(player2.id, Card(Sei, Bastoni)),  // Asso Spade, Re Denari, Sei Bastoni
      CardDealt(player3.id, Card(Tre, Spade)),    // Sette Denari, Cinque Coppe, Tre Spade
      CardDealt(player4.id, Card(Tre, Denari)),   // Quattro Spade, Asso Bastoni, Tre Denari
      
      TrumpRevealed(Card(Asso, Coppe)),

      CardPlayed(player1.id, Card(Due, Bastoni)),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      CardPlayed(player3.id, Card(Sette, Denari)),
      CardPlayed(player4.id, Card(Asso, Bastoni)),
      TrickWinner(player4.id),  // 11

      CardDealt(player4.id, Card(Fante, Bastoni)),  // Quattro Spade, Tre Denari, Fante Bastoni
      CardDealt(player1.id, Card(Due, Denari)),     // Sei Denari, Cinque Spade, Due Denari
      CardDealt(player2.id, Card(Fante, Spade)),    // Asso Spade, Re Denari, Fante Spade
      CardDealt(player3.id, Card(Re, Bastoni)),     // Cinque Coppe, Tre Spade, Re Bastoni
      CardPlayed(player4.id, Card(Fante, Bastoni)),
      CardPlayed(player1.id, Card(Due, Denari)),
      CardPlayed(player2.id, Card(Fante, Spade)),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      TrickWinner(player3.id),  // 8

      CardDealt(player3.id, Card(Sette, Bastoni)),  // Cinque Coppe, Tre Spade, Sette Bastoni
      CardDealt(player4.id, Card(Tre, Coppe)),      // Quattro Spade, Tre Denari, Tre Coppe
      CardDealt(player1.id, Card(Fante, Coppe)),    // Sei Denari, Cinque Spade, Fante Coppe
      CardDealt(player2.id, Card(Cinque, Bastoni)), // Asso Spade, Re Denari, Cinque Bastoni
      CardPlayed(player3.id, Card(Sette, Bastoni)),
      CardPlayed(player4.id, Card(Quattro, Spade)),
      CardPlayed(player1.id, Card(Sei, Denari)),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      TrickWinner(player3.id),  // 0 + 8 = 8

      CardDealt(player3.id, Card(Sei, Coppe)),        // Cinque Coppe, Tre Spade, Sei Coppe
      CardDealt(player4.id, Card(Cavallo, Denari)),   // Tre Denari, Tre Coppe, Cavallo Denari
      CardDealt(player1.id, Card(Cavallo, Bastoni)),  // Cinque Spade, Fante Coppe, Cavallo Bastoni
      CardDealt(player2.id, Card(Due, Coppe)),        // Asso Spade, Re Denari, Due Coppe
      CardPlayed(player3.id, Card(Sei, Coppe)),
      CardPlayed(player4.id, Card(Cavallo, Denari)),
      CardPlayed(player1.id, Card(Cavallo, Bastoni)),
      CardPlayed(player2.id, Card(Re, Denari)),
      TrickWinner(player3.id),  // 10 + 8 = 18

      CardDealt(player3.id, Card(Fante, Denari)),     // Cinque Coppe, Tre Spade, Fante Denari
      CardDealt(player4.id, Card(Cavallo, Spade)),    // Tre Denari, Tre Coppe, Cavallo Spade
      CardDealt(player1.id, Card(Quattro, Bastoni)),  // Cinque Spade, Fante Coppe, Quattro Bastoni
      CardDealt(player2.id, Card(Re, Coppe)),         // Asso Spade, Due Coppe, Re Coppe
      CardPlayed(player3.id, Card(Fante, Denari)),
      CardPlayed(player4.id, Card(Tre, Denari)),
      CardPlayed(player1.id, Card(Quattro, Bastoni)),
      CardPlayed(player2.id, Card(Re, Coppe)),
      TrickWinner(player2.id),  // 16 + 11 = 27

      CardDealt(player2.id, Card(Quattro, Coppe)),  // Asso Spade, Due Coppe, Quattro Coppe
      CardDealt(player3.id, Card(Asso, Denari)),    // Cinque Coppe, Tre Spade, Asso Denari
      CardDealt(player4.id, Card(Sette, Spade)),    // Tre Coppe, Cavallo Spade, Sette Spade
      CardDealt(player1.id, Card(Cinque, Denari)),  // Cinque Spade, Fante Coppe, Cinque Denari
      CardPlayed(player2.id, Card(Due, Coppe)),
      CardPlayed(player3.id, Card(Cinque, Coppe)),
      CardPlayed(player4.id, Card(Sette, Spade)),
      CardPlayed(player1.id, Card(Cinque, Spade)),
      TrickWinner(player3.id),  // 0 + 18 = 18

      CardDealt(player3.id, Card(Sette, Coppe)),    // Tre Spade, Asso Denari, Sette Coppe
      CardDealt(player4.id, Card(Re, Spade)),       // Tre Coppe, Cavallo Spade, Re Spade
      CardDealt(player1.id, Card(Sei, Spade)),      // Fante Coppe, Cinque Denari, Sei Spade
      CardDealt(player2.id, Card(Quattro, Denari)), // Asso Spade, Quattro Coppe, Quattro Denari
      CardPlayed(player3.id, Card(Sette, Coppe)),
      CardPlayed(player4.id, Card(Cavallo, Spade)),
      CardPlayed(player1.id, Card(Sei, Spade)),
      CardPlayed(player2.id, Card(Quattro, Denari)),
      TrickWinner(player3.id),  // 3 + 18 = 21

      CardDealt(player3.id, Card(Tre, Bastoni)),      // Tre Spade, Asso Denari, Tre Bastoni
      CardDealt(player4.id, Card(Due, Spade)),        // Tre Coppe, Re Spade, Due Spade
      CardDealt(player1.id, Card(Cavallo, Coppe)),    // Fante Coppe, Cinque Denari, Cavallo Coppe
      CardDealt(player2.id, Card(Asso, Coppe)),       // Asso Spade, Quattro Coppe, Asso Coppe

      CardPlayed(player3.id, Card(Tre, Bastoni)),      // Tre Spade, Asso Denari, ***
      CardPlayed(player4.id, Card(Re, Spade)),         // Tre Coppe, ***, Due Spade
      CardPlayed(player1.id, Card(Cavallo, Coppe)),    // Fante Coppe, Cinque Denari, ***
      CardPlayed(player2.id, Card(Quattro, Coppe)),    // Asso Spade, ***, Asso Coppe
      TrickWinner(player1.id),  // 17 + 21 = 38

      CardPlayed(player1.id, Card(Cinque, Denari)),    // Fante Coppe, ***, ***
      CardPlayed(player2.id, Card(Asso, Spade)),       // ***, ***, Asso Coppe
      CardPlayed(player3.id, Card(Asso, Denari)),      // Tre Spade, ***, ***
      CardPlayed(player4.id, Card(Due, Spade)),        // Tre Coppe, ***, ***
      TrickWinner(player3.id),  // 22 + 38 = 60

      CardPlayed(player3.id, Card(Tre, Spade)),
      CardPlayed(player4.id, Card(Tre, Coppe)),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      CardPlayed(player2.id, Card(Asso, Coppe)),
      TrickWinner(player2.id),  // 33 + 27 = 60

      PointsCount(List(player2.id, player4.id), 60),
      PointsCount(List(player3.id, player1.id), 60),
      MatchDraw
    )
  }
