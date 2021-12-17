package bastoni.backend

import bastoni.backend.briscola.Game
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

      ).map(Message(roomId, _))

    Game.playMatch[fs2.Pure](room)(input).map(_.message).compile.toList shouldBe List(
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

      PointsCount(List(player1.id), 68),
      PointsCount(List(player2.id), 52),
      MatchWinners(List(player1.id))
    )
  }

  "Irrelevant messages are ignored" in {
    val input = fs2.Stream(
      Message(room.id, ShuffleDeck(10)),
      Message(room.id, ShuffleDeck(1)),   // ignored (already shuffled)
      Message(room.id, drawCard),
      Message(room.id, drawCard),
      Message(room.id, drawCard),
      Message(room.id, drawCard),
      Message(room.id, drawCard),
      Message(room.id, drawCard),
      Message(room.id, revealTrump),
      Message(room.id, Continue),      // ignored, waiting for a player to play
      Message(room.id, PlayCard(player2.id, Card(Asso, Spade))),       // ignored (not your turn)
      Message(room.id, PlayCard(player1.id, Card(Asso, Spade))),       // ignored (not your card)
      Message(RoomId.newId, PlayCard(player1.id, Card(Due, Bastoni))), // ignored (different room)
    )

    Game.playMatch[fs2.Pure](room)(input).compile.toList shouldBe List(
      Message(room.id, DeckShuffled(10)),
      Message(room.id, CardDealt(player1.id, Card(Due, Bastoni))),
      Message(room.id, CardDealt(player2.id, Card(Asso, Spade))),
      Message(room.id, CardDealt(player1.id, Card(Sette, Denari))),
      Message(room.id, CardDealt(player2.id, Card(Quattro, Spade))),
      Message(room.id, CardDealt(player1.id, Card(Sei, Denari))),
      Message(room.id, CardDealt(player2.id, Card(Re, Denari))),
      Message(room.id, TrumpRevealed(Card(Cinque, Coppe)))
    )
  }

  "Game is aborted if a player leaves" in {
    val input = fs2.Stream(
      Message(room.id, ShuffleDeck(10)),
      Message(room.id, drawCard),
      Message(room.id, PlayerLeft(player1, Room(room.id, List(player2)))),
      Message(room.id, drawCard), // too late, game was aborted
    )

    Game.playMatch[fs2.Pure](room)(input).compile.toList shouldBe List(
      Message(room.id, DeckShuffled(10)),
      Message(room.id, CardDealt(player1.id, Card(Due, Bastoni))),
      Message(room.id, MatchAborted)
    )
  }