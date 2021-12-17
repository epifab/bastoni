package bastoni.backend

import bastoni.domain.*
import bastoni.domain.Rank.*
import bastoni.domain.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola3Spec extends AnyFreeSpec with Matchers:

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")

  val roomId = RoomId.newId
  val room = Room(roomId, List(player1, player2, player3))

  "A game can be played" in {
    val input =
      fs2.Stream(
        ShuffleDeck(10),

        DrawCard(player1),
        DrawCard(player2),
        DrawCard(player3),
        DrawCard(player1),
        DrawCard(player2),
        DrawCard(player3),
        DrawCard(player1),
        DrawCard(player2),
        DrawCard(player3),

        PlayCard(player1, Card(Quattro, Spade)),
        PlayCard(player2, Card(Asso, Spade)),
        PlayCard(player3, Card(Cinque, Spade)),

        DrawCard(player2),
        DrawCard(player3),
        DrawCard(player1),
        PlayCard(player2, Card(Sei, Denari)),
        PlayCard(player3, Card(Sette, Denari)),
        PlayCard(player1, Card(Cinque, Coppe)),

        DrawCard(player3),
        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player3, Card(Fante, Bastoni)),
        PlayCard(player1, Card(Due, Denari)),
        PlayCard(player2, Card(Fante, Spade)),

        DrawCard(player3),
        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player3, Card(Re, Denari)),
        PlayCard(player1, Card(Due, Bastoni)),
        PlayCard(player2, Card(Tre, Spade)),

        DrawCard(player1),
        DrawCard(player2),
        DrawCard(player3),
        PlayCard(player1, Card(Fante, Coppe)),
        PlayCard(player2, Card(Cinque, Bastoni)),
        PlayCard(player3, Card(Sei, Coppe)),

        DrawCard(player2),
        DrawCard(player3),
        DrawCard(player1),
        PlayCard(player2, Card(Cavallo, Denari)),
        PlayCard(player3, Card(Tre, Denari)),
        PlayCard(player1, Card(Sette, Bastoni)),

        DrawCard(player1),
        DrawCard(player2),
        DrawCard(player3),
        PlayCard(player1, Card(Cavallo, Spade)),
        PlayCard(player2, Card(Quattro, Bastoni)),
        PlayCard(player3, Card(Cavallo, Bastoni)),

        DrawCard(player3),
        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player3, Card(Quattro, Coppe)),
        PlayCard(player1, Card(Asso, Coppe)),
        PlayCard(player2, Card(Asso, Bastoni)),

        DrawCard(player2),
        DrawCard(player3),
        DrawCard(player1),
        PlayCard(player2, Card(Cinque, Denari)),
        PlayCard(player3, Card(Sette, Coppe)),
        PlayCard(player1, Card(Re, Spade)),

        DrawCard(player2),
        DrawCard(player3),
        DrawCard(player1),
        PlayCard(player2, Card(Sei, Spade)),
        PlayCard(player3, Card(Quattro, Denari)),
        PlayCard(player1, Card(Fante, Denari)),

        DrawCard(player2),
        DrawCard(player3),
        DrawCard(player1),
        PlayCard(player2, Card(Due, Spade)),
        PlayCard(player3, Card(Cavallo, Coppe)),
        PlayCard(player1, Card(Sei, Bastoni)),

        PlayCard(player1, Card(Asso, Denari)),
        PlayCard(player2, Card(Sette, Spade)),
        PlayCard(player3, Card(Re, Bastoni)),

        PlayCard(player3, Card(Re, Coppe)),
        PlayCard(player1, Card(Tre, Bastoni)),
        PlayCard(player2, Card(Tre, Coppe)),
      ).map(Message(roomId, _))

    Briscola[fs2.Pure](room, input).map(_.message).compile.toList shouldBe List(
      DeckShuffled(10),

      CardDealt(player1.id, Card(Due, Bastoni)),
      CardDealt(player2.id, Card(Asso, Spade)),
      CardDealt(player3.id, Card(Sette, Denari)),
      CardDealt(player1.id, Card(Quattro, Spade)),
      CardDealt(player2.id, Card(Sei, Denari)),
      CardDealt(player3.id, Card(Re, Denari)),
      CardDealt(player1.id, Card(Cinque, Coppe)),     // Due Bastoni, Quattro Spade, Cinque Coppe
      CardDealt(player2.id, Card(Asso, Bastoni)),     // Asso Spade, Sei Denari, Asso Bastoni
      CardDealt(player3.id, Card(Cinque, Spade)),     // Sette Denari, Re Denari, Cinque Spade

      TrumpRevealed(Card(Sei, Bastoni)),

      CardPlayed(player1.id, Card(Quattro, Spade)),
      CardPlayed(player2.id, Card(Asso, Spade)),
      CardPlayed(player3.id, Card(Cinque, Spade)),
      TrickWinner(player2.id),  // 11

      CardDealt(player2.id, Card(Tre, Spade)),        // Sei Denari, Asso Bastoni, Tre Spade
      CardDealt(player3.id, Card(Tre, Denari)),       // Sette Denari, Re Denari, Tre Denari
      CardDealt(player1.id, Card(Asso, Coppe)),       // Due Bastoni, Cinque Coppe, Asso Coppe
      CardPlayed(player2.id, Card(Sei, Denari)),
      CardPlayed(player3.id, Card(Sette, Denari)),
      CardPlayed(player1.id, Card(Cinque, Coppe)),
      TrickWinner(player3.id),  // 0

      CardDealt(player3.id, Card(Fante, Bastoni)),    // Re Denari, Tre Denari, Fante Bastoni
      CardDealt(player1.id, Card(Due, Denari)),       // Due Bastoni, Asso Coppe, Due Denari
      CardDealt(player2.id, Card(Fante, Spade)),      // Asso Bastoni, Tre Spade, Fante Spade
      CardPlayed(player3.id, Card(Fante, Bastoni)),
      CardPlayed(player1.id, Card(Due, Denari)),
      CardPlayed(player2.id, Card(Fante, Spade)),
      TrickWinner(player3.id),  // 4

      CardDealt(player3.id, Card(Re, Bastoni)),       // Re Denari, Tre Denari, Re Bastoni
      CardDealt(player1.id, Card(Sette, Bastoni)),    // Due Bastoni, Asso Coppe, Sette Bastoni
      CardDealt(player2.id, Card(Tre, Coppe)),        // Asso Bastoni, Tre Spade, Tre Coppe
      CardPlayed(player3.id, Card(Re, Denari)),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      CardPlayed(player2.id, Card(Tre, Spade)),
      TrickWinner(player1.id),  // 14

      CardDealt(player1.id, Card(Fante, Coppe)),      // Asso Coppe, Sette Bastoni, Fante Coppe
      CardDealt(player2.id, Card(Cinque, Bastoni)),   // Asso Bastoni, Cinque Bastoni, Tre Coppe
      CardDealt(player3.id, Card(Sei, Coppe)),        // Tre Denari, Re Bastoni, Sei Coppe
      CardPlayed(player1.id, Card(Fante, Coppe)),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      CardPlayed(player3.id, Card(Sei, Coppe)),
      TrickWinner(player2.id),  // 2

      CardDealt(player2.id, Card(Cavallo, Denari)),   // Asso Bastoni, Tre Coppe, Cavallo Denari
      CardDealt(player3.id, Card(Cavallo, Bastoni)),  // Tre Denari, Re Bastoni, Cavallo Bastoni
      CardDealt(player1.id, Card(Fante, Denari)),     // Asso Coppe, Sette Bastoni, Fante Denari
      CardPlayed(player2.id, Card(Cavallo, Denari)),
      CardPlayed(player3.id, Card(Tre, Denari)),
      CardPlayed(player1.id, Card(Sette, Bastoni)),
      TrickWinner(player1.id),  // 13

      CardDealt(player1.id, Card(Cavallo, Spade)),   // Asso Coppe, Fante Denari, Cavallo Spade
      CardDealt(player2.id, Card(Quattro, Bastoni)), // Asso Bastoni, Tre Coppe, Quattro Bastoni
      CardDealt(player3.id, Card(Re, Coppe)),        // Re Bastoni, Cavallo Bastoni, Re Coppe
      CardPlayed(player1.id, Card(Cavallo, Spade)),
      CardPlayed(player2.id, Card(Quattro, Bastoni)),
      CardPlayed(player3.id, Card(Cavallo, Bastoni)),
      TrickWinner(player3.id),  // 6

      CardDealt(player3.id, Card(Quattro, Coppe)),  // Re Bastoni, Re Coppe, Quattro Coppe
      CardDealt(player1.id, Card(Asso, Denari)),    // Asso Coppe, Fante Denari, Asso Denari
      CardDealt(player2.id, Card(Sette, Spade)),    // Asso Bastoni, Tre Coppe, Sette Spade
      CardPlayed(player3.id, Card(Quattro, Coppe)),
      CardPlayed(player1.id, Card(Asso, Coppe)),
      CardPlayed(player2.id, Card(Asso, Bastoni)),
      TrickWinner(player2.id),  // 22

      CardDealt(player2.id, Card(Cinque, Denari)),  // Tre Coppe, Sette Spade, Cinque Denari
      CardDealt(player3.id, Card(Sette, Coppe)),    // Re Bastoni, Re Coppe, Sette Coppe
      CardDealt(player1.id, Card(Re, Spade)),       // Fante Denari, Asso Denari, Re Spade
      CardPlayed(player2.id, Card(Cinque, Denari)),
      CardPlayed(player3.id, Card(Sette, Coppe)),
      CardPlayed(player1.id, Card(Re, Spade)),
      TrickWinner(player2.id),  // 4

      CardDealt(player2.id, Card(Sei, Spade)),      // Tre Coppe, Sette Spade, Sei Spade
      CardDealt(player3.id, Card(Quattro, Denari)), // Re Bastoni, Re Coppe, Quattro Denari
      CardDealt(player1.id, Card(Tre, Bastoni)),    // Fante Denari, Asso Denari, Tre Bastoni
      CardPlayed(player2.id, Card(Sei, Spade)),
      CardPlayed(player3.id, Card(Quattro, Denari)),
      CardPlayed(player1.id, Card(Fante, Denari)),
      TrickWinner(player2.id),  // 0

      CardDealt(player2.id, Card(Due, Spade)),      // Tre Coppe, Sette Spade, Due Spade
      CardDealt(player3.id, Card(Cavallo, Coppe)),  // Re Bastoni, Re Coppe, Cavallo Coppe
      CardDealt(player1.id, Card(Sei, Bastoni)),    // Asso Denari, Tre Bastoni, Sei Bastoni
      CardPlayed(player2.id, Card(Due, Spade)),
      CardPlayed(player3.id, Card(Cavallo, Coppe)),
      CardPlayed(player1.id, Card(Sei, Bastoni)),
      TrickWinner(player1.id),  // 3

      CardPlayed(player1.id, Card(Asso, Denari)),
      CardPlayed(player2.id, Card(Sette, Spade)),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      TrickWinner(player3.id),  // 15

      CardPlayed(player3.id, Card(Re, Coppe)),
      CardPlayed(player1.id, Card(Tre, Bastoni)),
      CardPlayed(player2.id, Card(Tre, Coppe)),
      TrickWinner(player1.id),  // 24

      PointsCount(player1.id, 54),
      PointsCount(player2.id, 41),
      PointsCount(player3.id, 25),
      MatchWinner(player1.id)
    )
  }
