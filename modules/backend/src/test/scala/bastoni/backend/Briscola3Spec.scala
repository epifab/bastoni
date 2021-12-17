package bastoni.backend

import bastoni.backend.briscola.Game
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
    val inputStream = Briscola3Spec.input(roomId, player1, player2, player3)
    val expectedOut = Briscola3Spec.output(roomId, player1, player2, player3)
    Game.playMatch[fs2.Pure](room)(inputStream).compile.toList shouldBe expectedOut
  }

object Briscola3Spec:

  val drawCard      = Continue
  val revealTrump   = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  val shortDelay = DelayedCommand(Continue, Delay.Short)
  val mediumDelay = DelayedCommand(Continue, Delay.Medium)
  val longDelay = DelayedCommand(Continue, Delay.Long)

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
    ).map(Message(roomId, _))

  def output(roomId: RoomId, player1: Player, player2: Player, player3: Player): List[Message | DelayedMessage] =
    List[Event | DelayedCommand](
      DeckShuffled(10),

      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni)),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade)),
      shortDelay,
      CardDealt(player3.id, Card(Sette, Denari)),
      shortDelay,
      CardDealt(player1.id, Card(Quattro, Spade)),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Denari)),
      shortDelay,
      CardDealt(player3.id, Card(Re, Denari)),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Coppe)),     // Due Bastoni, Quattro Spade, Cinque Coppe
      shortDelay,
      CardDealt(player2.id, Card(Asso, Bastoni)),     // Asso Spade, Sei Denari, Asso Bastoni
      shortDelay,
      CardDealt(player3.id, Card(Cinque, Spade)),     // Sette Denari, Re Denari, Cinque Spade

      mediumDelay,
      TrumpRevealed(Card(Sei, Bastoni)),

      CardPlayed(player1.id, Card(Quattro, Spade)),
      CardPlayed(player2.id, Card(Asso, Spade)),
      CardPlayed(player3.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickWinner(player2.id),  // 11

      mediumDelay,
      CardDealt(player2.id, Card(Tre, Spade)),        // Sei Denari, Asso Bastoni, Tre Spade
      shortDelay,
      CardDealt(player3.id, Card(Tre, Denari)),       // Sette Denari, Re Denari, Tre Denari
      shortDelay,
      CardDealt(player1.id, Card(Asso, Coppe)),       // Due Bastoni, Cinque Coppe, Asso Coppe
      CardPlayed(player2.id, Card(Sei, Denari)),
      CardPlayed(player3.id, Card(Sette, Denari)),
      CardPlayed(player1.id, Card(Cinque, Coppe)),
      mediumDelay,
      TrickWinner(player3.id),  // 0

      mediumDelay,
      CardDealt(player3.id, Card(Fante, Bastoni)),    // Re Denari, Tre Denari, Fante Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Due, Denari)),       // Due Bastoni, Asso Coppe, Due Denari
      shortDelay,
      CardDealt(player2.id, Card(Fante, Spade)),      // Asso Bastoni, Tre Spade, Fante Spade
      CardPlayed(player3.id, Card(Fante, Bastoni)),
      CardPlayed(player1.id, Card(Due, Denari)),
      CardPlayed(player2.id, Card(Fante, Spade)),
      mediumDelay,
      TrickWinner(player3.id),  // 4

      mediumDelay,
      CardDealt(player3.id, Card(Re, Bastoni)),       // Re Denari, Tre Denari, Re Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Sette, Bastoni)),    // Due Bastoni, Asso Coppe, Sette Bastoni
      shortDelay,
      CardDealt(player2.id, Card(Tre, Coppe)),        // Asso Bastoni, Tre Spade, Tre Coppe
      CardPlayed(player3.id, Card(Re, Denari)),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      CardPlayed(player2.id, Card(Tre, Spade)),
      mediumDelay,
      TrickWinner(player1.id),  // 14

      mediumDelay,
      CardDealt(player1.id, Card(Fante, Coppe)),      // Asso Coppe, Sette Bastoni, Fante Coppe
      shortDelay,
      CardDealt(player2.id, Card(Cinque, Bastoni)),   // Asso Bastoni, Cinque Bastoni, Tre Coppe
      shortDelay,
      CardDealt(player3.id, Card(Sei, Coppe)),        // Tre Denari, Re Bastoni, Sei Coppe
      CardPlayed(player1.id, Card(Fante, Coppe)),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      CardPlayed(player3.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickWinner(player2.id),  // 2

      mediumDelay,
      CardDealt(player2.id, Card(Cavallo, Denari)),   // Asso Bastoni, Tre Coppe, Cavallo Denari
      shortDelay,
      CardDealt(player3.id, Card(Cavallo, Bastoni)),  // Tre Denari, Re Bastoni, Cavallo Bastoni
      shortDelay,
      CardDealt(player1.id, Card(Fante, Denari)),     // Asso Coppe, Sette Bastoni, Fante Denari
      CardPlayed(player2.id, Card(Cavallo, Denari)),
      CardPlayed(player3.id, Card(Tre, Denari)),
      CardPlayed(player1.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickWinner(player1.id),  // 13

      mediumDelay,
      CardDealt(player1.id, Card(Cavallo, Spade)),   // Asso Coppe, Fante Denari, Cavallo Spade
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Bastoni)), // Asso Bastoni, Tre Coppe, Quattro Bastoni
      shortDelay,
      CardDealt(player3.id, Card(Re, Coppe)),        // Re Bastoni, Cavallo Bastoni, Re Coppe
      CardPlayed(player1.id, Card(Cavallo, Spade)),
      CardPlayed(player2.id, Card(Quattro, Bastoni)),
      CardPlayed(player3.id, Card(Cavallo, Bastoni)),
      mediumDelay,
      TrickWinner(player3.id),  // 6

      mediumDelay,
      CardDealt(player3.id, Card(Quattro, Coppe)),  // Re Bastoni, Re Coppe, Quattro Coppe
      shortDelay,
      CardDealt(player1.id, Card(Asso, Denari)),    // Asso Coppe, Fante Denari, Asso Denari
      shortDelay,
      CardDealt(player2.id, Card(Sette, Spade)),    // Asso Bastoni, Tre Coppe, Sette Spade
      CardPlayed(player3.id, Card(Quattro, Coppe)),
      CardPlayed(player1.id, Card(Asso, Coppe)),
      CardPlayed(player2.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickWinner(player2.id),  // 22

      mediumDelay,
      CardDealt(player2.id, Card(Cinque, Denari)),  // Tre Coppe, Sette Spade, Cinque Denari
      shortDelay,
      CardDealt(player3.id, Card(Sette, Coppe)),    // Re Bastoni, Re Coppe, Sette Coppe
      shortDelay,
      CardDealt(player1.id, Card(Re, Spade)),       // Fante Denari, Asso Denari, Re Spade
      CardPlayed(player2.id, Card(Cinque, Denari)),
      CardPlayed(player3.id, Card(Sette, Coppe)),
      CardPlayed(player1.id, Card(Re, Spade)),
      mediumDelay,
      TrickWinner(player2.id),  // 4

      mediumDelay,
      CardDealt(player2.id, Card(Sei, Spade)),      // Tre Coppe, Sette Spade, Sei Spade
      shortDelay,
      CardDealt(player3.id, Card(Quattro, Denari)), // Re Bastoni, Re Coppe, Quattro Denari
      shortDelay,
      CardDealt(player1.id, Card(Tre, Bastoni)),    // Fante Denari, Asso Denari, Tre Bastoni
      CardPlayed(player2.id, Card(Sei, Spade)),
      CardPlayed(player3.id, Card(Quattro, Denari)),
      CardPlayed(player1.id, Card(Fante, Denari)),
      mediumDelay,
      TrickWinner(player2.id),  // 0

      mediumDelay,
      CardDealt(player2.id, Card(Due, Spade)),      // Tre Coppe, Sette Spade, Due Spade
      shortDelay,
      CardDealt(player3.id, Card(Cavallo, Coppe)),  // Re Bastoni, Re Coppe, Cavallo Coppe
      shortDelay,
      CardDealt(player1.id, Card(Sei, Bastoni)),    // Asso Denari, Tre Bastoni, Sei Bastoni
      CardPlayed(player2.id, Card(Due, Spade)),
      CardPlayed(player3.id, Card(Cavallo, Coppe)),
      CardPlayed(player1.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickWinner(player1.id),  // 3

      CardPlayed(player1.id, Card(Asso, Denari)),
      CardPlayed(player2.id, Card(Sette, Spade)),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickWinner(player3.id),  // 15

      CardPlayed(player3.id, Card(Re, Coppe)),
      CardPlayed(player1.id, Card(Tre, Bastoni)),
      CardPlayed(player2.id, Card(Tre, Coppe)),
      mediumDelay,
      TrickWinner(player1.id),  // 24

      longDelay,
      PointsCount(List(player1.id), 54),
      PointsCount(List(player2.id), 41),
      PointsCount(List(player3.id), 25),
      MatchWinners(List(player1.id))
    ).map(_.toMessage(roomId))
