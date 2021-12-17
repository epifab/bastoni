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

  val players = List(player1, player2, player3, player4)

  "A game can be played" ignore {
    val inputStream = Briscola4Spec.input(room1, player1, player2, player3, player4)
    val expectedOut = Briscola4Spec.output(room1, player1, player2, player3, player4)
    Game.playMatch[cats.Id](room1, players, messageId)(inputStream).compile.toList shouldBe expectedOut
  }

object Briscola4Spec:
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

      CardsDealt(player1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(player2.id, List(Card(Quattro, Spade), Card(Sei, Denari), Card(Re, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(player3.id, List(Card(Cinque, Coppe), Card(Asso, Bastoni), Card(Cinque, Spade)), Direction.Player),
      shortDelay,
      CardsDealt(player4.id, List(Card(Sei, Bastoni), Card(Tre, Spade), Card(Tre, Denari)), Direction.Player),
      mediumDelay,
      TrumpRevealed(Card(Asso, Coppe)),

      ActionRequested(player1.id, Action.PlayCard),
      willTick(-186329082),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1047710074),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-79111910),
      CardPlayed(player3.id, Card(Sette, Denari)),
      ActionRequested(player4.id, Action.PlayCard),
      willTick(29793542),
      CardPlayed(player4.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(player4.id),  // 11

      mediumDelay,
      CardsDealt(player4.id, List(Card(Fante, Bastoni)), Direction.Player),  // Quattro Spade, Tre Denari, Fante Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Due, Denari)), Direction.Player),     // Sei Denari, Cinque Spade, Due Denari
      shortDelay,
      CardsDealt(player2.id, List(Card(Fante, Spade)), Direction.Player),    // Asso Spade, Re Denari, Fante Spade
      shortDelay,
      CardsDealt(player3.id, List(Card(Re, Bastoni)), Direction.Player),     // Cinque Coppe, Tre Spade, Re Bastoni
      ActionRequested(player4.id, Action.PlayCard),
      willTick(366268412),
      CardPlayed(player4.id, Card(Fante, Bastoni)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1900311462),
      CardPlayed(player1.id, Card(Due, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-276900154),
      CardPlayed(player2.id, Card(Fante, Spade)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-1552847258),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 8

      mediumDelay,
      CardsDealt(player3.id, List(Card(Sette, Bastoni)), Direction.Player),  // Cinque Coppe, Tre Spade, Sette Bastoni
      shortDelay,
      CardsDealt(player4.id, List(Card(Tre, Coppe)), Direction.Player),      // Quattro Spade, Tre Denari, Tre Coppe
      shortDelay,
      CardsDealt(player1.id, List(Card(Fante, Coppe)), Direction.Player),    // Sei Denari, Cinque Spade, Fante Coppe
      shortDelay,
      CardsDealt(player2.id, List(Card(Cinque, Bastoni)), Direction.Player), // Asso Spade, Re Denari, Cinque Bastoni
      ActionRequested(player3.id, Action.PlayCard),
      willTick(318297128),
      CardPlayed(player3.id, Card(Sette, Bastoni)),
      ActionRequested(player4.id, Action.PlayCard),
      willTick(-1761588950),
      CardPlayed(player4.id, Card(Quattro, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1075756534),
      CardPlayed(player1.id, Card(Sei, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1301658324),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0 + 8 = 8

      mediumDelay,
      CardsDealt(player3.id, List(Card(Sei, Coppe)), Direction.Player),        // Cinque Coppe, Tre Spade, Sei Coppe
      shortDelay,
      CardsDealt(player4.id, List(Card(Cavallo, Denari)), Direction.Player),   // Tre Denari, Tre Coppe, Cavallo Denari
      shortDelay,
      CardsDealt(player1.id, List(Card(Cavallo, Bastoni)), Direction.Player),  // Cinque Spade, Fante Coppe, Cavallo Bastoni
      shortDelay,
      CardsDealt(player2.id, List(Card(Due, Coppe)), Direction.Player),        // Asso Spade, Re Denari, Due Coppe
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-343908206),
      CardPlayed(player3.id, Card(Sei, Coppe)),
      ActionRequested(player4.id, Action.PlayCard),
      willTick(1725731358),
      CardPlayed(player4.id, Card(Cavallo, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-509802342),
      CardPlayed(player1.id, Card(Cavallo, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-720268228),
      CardPlayed(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(player3.id),  // 10 + 8 = 18

      mediumDelay,
      CardsDealt(player3.id, List(Card(Fante, Denari)), Direction.Player),     // Cinque Coppe, Tre Spade, Fante Denari
      shortDelay,
      CardsDealt(player4.id, List(Card(Cavallo, Spade)), Direction.Player),    // Tre Denari, Tre Coppe, Cavallo Spade
      shortDelay,
      CardsDealt(player1.id, List(Card(Quattro, Bastoni)), Direction.Player),  // Cinque Spade, Fante Coppe, Quattro Bastoni
      shortDelay,
      CardsDealt(player2.id, List(Card(Re, Coppe)), Direction.Player),         // Asso Spade, Due Coppe, Re Coppe
      ActionRequested(player3.id, Action.PlayCard),
      willTick(1281485486),
      CardPlayed(player3.id, Card(Fante, Denari)),
      ActionRequested(player4.id, Action.PlayCard),
      willTick(1427366616),
      CardPlayed(player4.id, Card(Tre, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1173225188),
      CardPlayed(player1.id, Card(Quattro, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(569619490),
      CardPlayed(player2.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 16 + 11 = 27

      mediumDelay,
      CardsDealt(player2.id, List(Card(Quattro, Coppe)), Direction.Player),  // Asso Spade, Due Coppe, Quattro Coppe
      shortDelay,
      CardsDealt(player3.id, List(Card(Asso, Denari)), Direction.Player),    // Cinque Coppe, Tre Spade, Asso Denari
      shortDelay,
      CardsDealt(player4.id, List(Card(Sette, Spade)), Direction.Player),    // Tre Coppe, Cavallo Spade, Sette Spade
      shortDelay,
      CardsDealt(player1.id, List(Card(Cinque, Denari)), Direction.Player),  // Cinque Spade, Fante Coppe, Cinque Denari
      ActionRequested(player2.id, Action.PlayCard),
      willTick(469903836),
      CardPlayed(player2.id, Card(Due, Coppe)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(1598476816),
      CardPlayed(player3.id, Card(Cinque, Coppe)),
      ActionRequested(player4.id, Action.PlayCard),
      willTick(1534029950),
      CardPlayed(player4.id, Card(Sette, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-554207972),
      CardPlayed(player1.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0 + 18 = 18

      mediumDelay,
      CardsDealt(player3.id, List(Card(Sette, Coppe)), Direction.Player),    // Tre Spade, Asso Denari, Sette Coppe
      shortDelay,
      CardsDealt(player4.id, List(Card(Re, Spade)), Direction.Player),       // Tre Coppe, Cavallo Spade, Re Spade
      shortDelay,
      CardsDealt(player1.id, List(Card(Sei, Spade)), Direction.Player),      // Fante Coppe, Cinque Denari, Sei Spade
      shortDelay,
      CardsDealt(player2.id, List(Card(Quattro, Denari)), Direction.Player), // Asso Spade, Quattro Coppe, Quattro Denari
      ActionRequested(player3.id, Action.PlayCard),
      willTick(1392774646),
      CardPlayed(player3.id, Card(Sette, Coppe)),
      ActionRequested(player4.id, Action.PlayCard),
      willTick(-1681315332),
      CardPlayed(player4.id, Card(Cavallo, Spade)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(2024606492),
      CardPlayed(player1.id, Card(Sei, Spade)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1476503804),
      CardPlayed(player2.id, Card(Quattro, Denari)),
      mediumDelay,
      TrickCompleted(player3.id),  // 3 + 18 = 21

      mediumDelay,
      CardsDealt(player3.id, List(Card(Tre, Bastoni)), Direction.Player),      // Tre Spade, Asso Denari, Tre Bastoni
      shortDelay,
      CardsDealt(player4.id, List(Card(Due, Spade)), Direction.Player),        // Tre Coppe, Re Spade, Due Spade
      shortDelay,
      CardsDealt(player1.id, List(Card(Cavallo, Coppe)), Direction.Player),    // Fante Coppe, Cinque Denari, Cavallo Coppe
      shortDelay,
      CardsDealt(player2.id, List(Card(Asso, Coppe)), Direction.Player),       // Asso Spade, Quattro Coppe, Asso Coppe

      ActionRequested(player3.id, Action.PlayCard),
      willTick(1516183072),
      CardPlayed(player3.id, Card(Tre, Bastoni)),      // Tre Spade, Asso Denari, ***
      ActionRequested(player4.id, Action.PlayCard),
      willTick(-1803678264),
      CardPlayed(player4.id, Card(Re, Spade)),         // Tre Coppe, ***, Due Spade
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1367052060),
      CardPlayed(player1.id, Card(Cavallo, Coppe)),    // Fante Coppe, Cinque Denari, ***
      ActionRequested(player2.id, Action.PlayCard),
      willTick(988792452),
      CardPlayed(player2.id, Card(Quattro, Coppe)),    // Asso Spade, ***, Asso Coppe
      mediumDelay,
      TrickCompleted(player1.id),  // 17 + 21 = 38

      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1995445148),
      CardPlayed(player1.id, Card(Cinque, Denari)),    // Fante Coppe, ***, ***
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-2088425286),
      CardPlayed(player2.id, Card(Asso, Spade)),       // ***, ***, Asso Coppe
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-1364646424),
      CardPlayed(player3.id, Card(Asso, Denari)),      // Tre Spade, ***, ***
      ActionRequested(player4.id, Action.PlayCard),
      willTick(1259482758),
      CardPlayed(player4.id, Card(Due, Spade)),        // Tre Coppe, ***, ***
      mediumDelay,
      TrickCompleted(player3.id),  // 22 + 38 = 60

      ActionRequested(player3.id, Action.PlayCard),
      willTick(1125428124),
      CardPlayed(player3.id, Card(Tre, Spade)),
      ActionRequested(player4.id, Action.PlayCard),
      willTick(-1278364876),
      CardPlayed(player4.id, Card(Tre, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1958400302),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(957462704),
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
