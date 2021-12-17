package bastoni.backend

import bastoni.domain.*
import bastoni.domain.Rank.*
import bastoni.domain.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Briscola2Spec extends AnyFreeSpec with Matchers:

  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")
  val player3 = Player(PlayerId.newId, "Sempronio")

  val roomId = RoomId.newId
  val room = Room(roomId, List(player1, player2))

  "A game can be played" in {
    val input =
      fs2.Stream(
        ShuffleDeck(10),

        DrawCard(player1),
        DrawCard(player2),
        DrawCard(player1),
        DrawCard(player2),
        DrawCard(player1),
        DrawCard(player2),
        Continue,

        PlayCard(player1, Card(Due, Bastoni)),
        PlayCard(player2, Card(Quattro, Spade)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Sei, Denari)),
        PlayCard(player2, Card(Re, Denari)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Cinque, Spade)),
        PlayCard(player1, Card(Tre, Spade)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Sette, Denari)),
        PlayCard(player2, Card(Sei, Bastoni)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Fante, Bastoni)),
        PlayCard(player2, Card(Due, Denari)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Tre, Denari)),
        PlayCard(player2, Card(Asso, Coppe)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Sette, Bastoni)),
        PlayCard(player1, Card(Asso, Bastoni)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Fante, Spade)),
        PlayCard(player2, Card(Asso, Spade)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Cinque, Bastoni)),
        PlayCard(player1, Card(Cavallo, Denari)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Re, Bastoni)),
        PlayCard(player1, Card(Due, Coppe)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Fante, Denari)),
        PlayCard(player2, Card(Cavallo, Bastoni)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Quattro, Bastoni)),
        PlayCard(player2, Card(Cavallo, Spade)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Quattro, Coppe)),
        PlayCard(player2, Card(Sei, Coppe)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Sette, Spade)),
        PlayCard(player1, Card(Cinque, Denari)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Sette, Coppe)),
        PlayCard(player1, Card(Re, Spade)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Sei, Spade)),
        PlayCard(player1, Card(Quattro, Denari)),
        Continue,

        DrawCard(player2),
        DrawCard(player1),
        PlayCard(player2, Card(Tre, Bastoni)),
        PlayCard(player1, Card(Fante, Coppe)),
        Continue,

        DrawCard(player1),
        DrawCard(player2),
        PlayCard(player1, Card(Due, Spade)),
        PlayCard(player2, Card(Asso, Denari)),
        Continue,

        PlayCard(player1, Card(Cavallo, Coppe)),
        PlayCard(player2, Card(Re, Coppe)),
        Continue,

        PlayCard(player2, Card(Cinque, Coppe)),
        PlayCard(player1, Card(Tre, Coppe)),
        Continue,
        Continue,

      ).map(Message(roomId, _))

    Briscola[fs2.Pure](room, input).map(_.message).compile.toList shouldBe List(
      DeckShuffled(10),

      CardDealt(player1.id, Card(Due, Bastoni)),
      CardDealt(player2.id, Card(Asso, Spade)),
      CardDealt(player1.id, Card(Sette, Denari)),
      CardDealt(player2.id, Card(Quattro, Spade)),
      CardDealt(player1.id, Card(Sei, Denari)),
      CardDealt(player2.id, Card(Re, Denari)),

      TrumpRevealed(Card(Cinque, Coppe)),

      CardPlayed(player1.id, Card(Due, Bastoni)),
      CardPlayed(player2.id, Card(Quattro, Spade)),
      TrickWinner(player1.id),  // 0

      CardDealt(player1.id, Card(Asso, Bastoni)),     // Sette Denari, Sei Denari, Asso Bastoni
      CardDealt(player2.id, Card(Cinque, Spade)),     // Asso Spade, Re Denari, Cinque spade
      CardPlayed(player1.id, Card(Sei, Denari)),
      CardPlayed(player2.id, Card(Re, Denari)),
      TrickWinner(player2.id),  // 4

      CardDealt(player2.id, Card(Sei, Bastoni)),      // Asso Spade, Cinque Spade, Sei Bastoni
      CardDealt(player1.id, Card(Tre, Spade)),        // Sette Denari, Asso Bastoni, Tre Spade
      CardPlayed(player2.id, Card(Cinque, Spade)),
      CardPlayed(player1.id, Card(Tre, Spade)),
      TrickWinner(player1.id),  // 10

      CardDealt(player1.id, Card(Tre, Denari)),       // Sette Denari, Asso Bastoni, Tre Denari
      CardDealt(player2.id, Card(Asso, Coppe)),       // Asso Spade, Sei Bastoni, Asso Coppe
      CardPlayed(player1.id, Card(Sette, Denari)),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      TrickWinner(player1.id),  // 10

      CardDealt(player1.id, Card(Fante, Bastoni)),    // Asso Bastoni, Tre Denari, Fante Bastoni
      CardDealt(player2.id, Card(Due, Denari)),       // Asso Spade, Asso Coppe, Due Denari
      CardPlayed(player1.id, Card(Fante, Bastoni)),
      CardPlayed(player2.id, Card(Due, Denari)),
      TrickWinner(player1.id),  // 12

      CardDealt(player1.id, Card(Fante, Spade)),      // Asso Bastoni, Tre Denari, Fante Spade
      CardDealt(player2.id, Card(Re, Bastoni)),       // Asso Spade, Asso Coppe, Re Bastoni
      CardPlayed(player1.id, Card(Tre, Denari)),
      CardPlayed(player2.id, Card(Asso, Coppe)),
      TrickWinner(player2.id),  // 25

      CardDealt(player2.id, Card(Sette, Bastoni)),    // Asso Spade, Re Bastoni, Sette Bastoni
      CardDealt(player1.id, Card(Tre, Coppe)),        // Asso Bastoni, Fante Spade, Tre Coppe
      CardPlayed(player2.id, Card(Sette, Bastoni)),
      CardPlayed(player1.id, Card(Asso, Bastoni)),
      TrickWinner(player1.id),  // 23

      CardDealt(player1.id, Card(Fante, Coppe)),      // Fante Spade, Tre Coppe, Fante Coppe
      CardDealt(player2.id, Card(Cinque, Bastoni)),   // Asso Spade, Re Bastoni, Cinque Bastoni
      CardPlayed(player1.id, Card(Fante, Spade)),
      CardPlayed(player2.id, Card(Asso, Spade)),
      TrickWinner(player2.id),  // 38

      CardDealt(player2.id, Card(Sei, Coppe)),        // Re Bastoni, Cinque Bastoni, Sei Coppe
      CardDealt(player1.id, Card(Cavallo, Denari)),   // Tre Coppe, Fante Coppe, Cavallo Denari
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      CardPlayed(player1.id, Card(Cavallo, Denari)),
      TrickWinner(player2.id),  // 41

      CardDealt(player2.id, Card(Cavallo, Bastoni)),  // Re Bastoni, Sei Coppe, Cavallo Bastoni
      CardDealt(player1.id, Card(Due, Coppe)),        // Tre Coppe, Fante Coppe, Due Coppe
      CardPlayed(player2.id, Card(Re, Bastoni)),
      CardPlayed(player1.id, Card(Due, Coppe)),
      TrickWinner(player1.id),  // 27

      CardDealt(player1.id, Card(Fante, Denari)),     // Tre Coppe, Fante Coppe, Fante Denari
      CardDealt(player2.id, Card(Cavallo, Spade)),    // Sei Coppe, Cavallo Bastoni, Cavallo Spade
      CardPlayed(player1.id, Card(Fante, Denari)),
      CardPlayed(player2.id, Card(Cavallo, Bastoni)),
      TrickWinner(player1.id),  // 32

      CardDealt(player1.id, Card(Quattro, Bastoni)), // Tre Coppe, Fante Coppe, Quattro Bastoni
      CardDealt(player2.id, Card(Re, Coppe)),        // Sei Coppe, Cavallo Spade, Re Coppe
      CardPlayed(player1.id, Card(Quattro, Bastoni)),
      CardPlayed(player2.id, Card(Cavallo, Spade)),
      TrickWinner(player1.id),  // 35

      CardDealt(player1.id, Card(Quattro, Coppe)),   // Tre Coppe, Fante Coppe, Quattro Coppe
      CardDealt(player2.id, Card(Asso, Denari)),     // Sei Coppe, Re Coppe, Asso Denari
      CardPlayed(player1.id, Card(Quattro, Coppe)),
      CardPlayed(player2.id, Card(Sei, Coppe)),
      TrickWinner(player2.id),  // 41

      CardDealt(player2.id, Card(Sette, Spade)),    // Re Coppe, Asso Denari, Sette Spade
      CardDealt(player1.id, Card(Cinque, Denari)),  // Tre Coppe, Fante Coppe, Cinque Denari
      CardPlayed(player2.id, Card(Sette, Spade)),
      CardPlayed(player1.id, Card(Cinque, Denari)),
      TrickWinner(player2.id),  // 41

      CardDealt(player2.id, Card(Sette, Coppe)),    // Re Coppe, Asso Denari, Sette Coppe
      CardDealt(player1.id, Card(Re, Spade)),       // Tre Coppe, Fante Coppe, Re Spade
      CardPlayed(player2.id, Card(Sette, Coppe)),
      CardPlayed(player1.id, Card(Re, Spade)),
      TrickWinner(player2.id),  // 45

      CardDealt(player2.id, Card(Sei, Spade)),      // Re Coppe, Asso Denari, Sei Spade
      CardDealt(player1.id, Card(Quattro, Denari)), // Tre Coppe, Fante Coppe, Quattro Denari
      CardPlayed(player2.id, Card(Sei, Spade)),
      CardPlayed(player1.id, Card(Quattro, Denari)),
      TrickWinner(player2.id),  // 45

      CardDealt(player2.id, Card(Tre, Bastoni)),    // Re Coppe, Asso Denari, Tre Bastoni
      CardDealt(player1.id, Card(Due, Spade)),      // Tre Coppe, Fante Coppe, Due Spade
      CardPlayed(player2.id, Card(Tre, Bastoni)),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      TrickWinner(player1.id),  // 47

      CardDealt(player1.id, Card(Cavallo, Coppe)),  // Tre Coppe, Due Spade, Cavallo Coppe
      CardDealt(player2.id, Card(Cinque, Coppe)),   // Re Coppe, Asso Denari, Cinque Coppe
      CardPlayed(player1.id, Card(Due, Spade)),
      CardPlayed(player2.id, Card(Asso, Denari)),
      TrickWinner(player1.id),  // 58

      CardPlayed(player1.id, Card(Cavallo, Coppe)),
      CardPlayed(player2.id, Card(Re, Coppe)),
      TrickWinner(player2.id),  // 52

      CardPlayed(player2.id, Card(Cinque, Coppe)),
      CardPlayed(player1.id, Card(Tre, Coppe)),
      TrickWinner(player1.id),  // 68

      PointsCount(player1.id, 68),
      PointsCount(player2.id, 52),
      MatchWinner(player1.id)
    )
  }

  "Irrelevant messages are ignored" in {
    val input = fs2.Stream(
      Message(room.id, ShuffleDeck(10)),
      Message(room.id, ShuffleDeck(1)),         // ignored (already shuffled)
      Message(room.id, DrawCard(player1)),
      Message(room.id, DrawCard(player1)),      // ignored (not your turn)
      Message(room.id, DrawCard(player3)),      // ignored (not in the room)
      Message(room.id, DrawCard(player2)),
      Message(room.id, PlayCard(player2, Card(Asso, Spade))),       // ignored (not your turn)
      Message(room.id, PlayCard(player2, Card(Due, Bastoni))),         // ignored (not your card)
      Message(RoomId.newId, PlayCard(player1, Card(Due, Bastoni))),    // ignored (different room)
    )

    Briscola[fs2.Pure](room, input).compile.toList shouldBe List(
      Message(room.id, DeckShuffled(10)),
      Message(room.id, CardDealt(player1.id, Card(Due, Bastoni))),
      Message(room.id, CardDealt(player2.id, Card(Asso, Spade))),
    )
  }

  "Game is aborted if a player leaves" in {
    val input = fs2.Stream(
      Message(room.id, ShuffleDeck(10)),
      Message(room.id, DrawCard(player1)),
      Message(room.id, PlayerLeft(player1, Room(room.id, List(player2)))),
      Message(room.id, DrawCard(player2)), // too late, game was aborted
    )

    Briscola[fs2.Pure](room, input).compile.toList shouldBe List(
      Message(room.id, DeckShuffled(10)),
      Message(room.id, CardDealt(player1.id, Card(Due, Bastoni))),
      Message(room.id, GameAborted)
    )
  }