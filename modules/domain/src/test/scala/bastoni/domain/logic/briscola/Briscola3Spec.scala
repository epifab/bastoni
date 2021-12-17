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

class Briscola3Spec extends AnyFreeSpec with Matchers:
  val players = List(player1, player2, player3)

  "A game can be played" in {
    val inputStream = Briscola3Spec.input(room1, player1, player2, player3)
    val expectedOut = Briscola3Spec.output(room1, GamePlayer(player1, 0), GamePlayer(player2, 0), GamePlayer(player3, 0))
    Game.playMatch[cats.Id](room1, players, messageId)(inputStream).compile.toList shouldBe expectedOut
  }

object Briscola3Spec:

  def input(roomId: RoomId, player1: Player, player2: Player, player3: Player): fs2.Stream[fs2.Pure, Message] =
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

  def output(roomId: RoomId, player1: GamePlayer, player2: GamePlayer, player3: GamePlayer): List[Message | Delayed[Message]] =
    List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck.filterNot(_ == Card(Due, Coppe))),

      mediumDelay,
      CardsDealt(player1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(player2.id, List(Card(Quattro, Spade), Card(Sei, Denari), Card(Re, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(player3.id, List(Card(Cinque, Coppe), Card(Asso, Bastoni), Card(Cinque, Spade)), Direction.Player),

      mediumDelay,
      TrumpRevealed(Card(Sei, Bastoni)),

      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1850767521),
      CardPlayed(player1.id, Card(Quattro, Spade)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(666922441),
      CardPlayed(player2.id, Card(Asso, Spade)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-563876707),
      CardPlayed(player3.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 11

      mediumDelay,
      CardsDealt(player2.id, List(Card(Tre, Spade)), Direction.Player),        // Sei Denari, Asso Bastoni, Tre Spade
      shortDelay,
      CardsDealt(player3.id, List(Card(Tre, Denari)), Direction.Player),       // Sette Denari, Re Denari, Tre Denari
      shortDelay,
      CardsDealt(player1.id, List(Card(Asso, Coppe)), Direction.Player),       // Due Bastoni, Cinque Coppe, Asso Coppe
      ActionRequested(player2.id, Action.PlayCard),
      willTick(193603333),
      CardPlayed(player2.id, Card(Sei, Denari)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-2087661231),
      CardPlayed(player3.id, Card(Sette, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(97785381),
      CardPlayed(player1.id, Card(Cinque, Coppe)),
      mediumDelay,
      TrickCompleted(player3.id),  // 0

      mediumDelay,
      CardsDealt(player3.id, List(Card(Fante, Bastoni)), Direction.Player),    // Re Denari, Tre Denari, Fante Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Due, Denari)), Direction.Player),       // Due Bastoni, Asso Coppe, Due Denari
      shortDelay,
      CardsDealt(player2.id, List(Card(Fante, Spade)), Direction.Player),      // Asso Bastoni, Tre Spade, Fante Spade
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-1763492531),
      CardPlayed(player3.id, Card(Fante, Bastoni)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1866131659),
      CardPlayed(player1.id, Card(Due, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1746981073),
      CardPlayed(player2.id, Card(Fante, Spade)),
      mediumDelay,
      TrickCompleted(player3.id),  // 4

      mediumDelay,
      CardsDealt(player3.id, List(Card(Re, Bastoni)), Direction.Player),       // Re Denari, Tre Denari, Re Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Sette, Bastoni)), Direction.Player),    // Due Bastoni, Asso Coppe, Sette Bastoni
      shortDelay,
      CardsDealt(player2.id, List(Card(Tre, Coppe)), Direction.Player),        // Asso Bastoni, Tre Spade, Tre Coppe
      ActionRequested(player3.id, Action.PlayCard),
      willTick(853709817),
      CardPlayed(player3.id, Card(Re, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(189038673),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-195494413),
      CardPlayed(player2.id, Card(Tre, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),  // 14

      mediumDelay,
      CardsDealt(player1.id, List(Card(Fante, Coppe)), Direction.Player),      // Asso Coppe, Sette Bastoni, Fante Coppe
      shortDelay,
      CardsDealt(player2.id, List(Card(Cinque, Bastoni)), Direction.Player),   // Asso Bastoni, Cinque Bastoni, Tre Coppe
      shortDelay,
      CardsDealt(player3.id, List(Card(Sei, Coppe)), Direction.Player),        // Tre Denari, Re Bastoni, Sei Coppe
      ActionRequested(player1.id, Action.PlayCard),
      willTick(802663755),
      CardPlayed(player1.id, Card(Fante, Coppe)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-109651465),
      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(663950809),
      CardPlayed(player3.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),  // 2

      mediumDelay,
      CardsDealt(player2.id, List(Card(Cavallo, Denari)), Direction.Player),   // Asso Bastoni, Tre Coppe, Cavallo Denari
      shortDelay,
      CardsDealt(player3.id, List(Card(Cavallo, Bastoni)), Direction.Player),  // Tre Denari, Re Bastoni, Cavallo Bastoni
      shortDelay,
      CardsDealt(player1.id, List(Card(Fante, Denari)), Direction.Player),     // Asso Coppe, Sette Bastoni, Fante Denari
      ActionRequested(player2.id, Action.PlayCard),
      willTick(983387907),
      CardPlayed(player2.id, Card(Cavallo, Denari)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(-52348511),
      CardPlayed(player3.id, Card(Tre, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(1877714639),
      CardPlayed(player1.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 13

      mediumDelay,
      CardsDealt(player1.id, List(Card(Cavallo, Spade)), Direction.Player),   // Asso Coppe, Fante Denari, Cavallo Spade
      shortDelay,
      CardsDealt(player2.id, List(Card(Quattro, Bastoni)), Direction.Player), // Asso Bastoni, Tre Coppe, Quattro Bastoni
      shortDelay,
      CardsDealt(player3.id, List(Card(Re, Coppe)), Direction.Player),        // Re Bastoni, Cavallo Bastoni, Re Coppe
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-46933631),
      CardPlayed(player1.id, Card(Cavallo, Spade)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(1967325695),
      CardPlayed(player2.id, Card(Quattro, Bastoni)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(1334594719),
      CardPlayed(player3.id, Card(Cavallo, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 6

      mediumDelay,
      CardsDealt(player3.id, List(Card(Quattro, Coppe)), Direction.Player),  // Re Bastoni, Re Coppe, Quattro Coppe
      shortDelay,
      CardsDealt(player1.id, List(Card(Asso, Denari)), Direction.Player),    // Asso Coppe, Fante Denari, Asso Denari
      shortDelay,
      CardsDealt(player2.id, List(Card(Sette, Spade)), Direction.Player),    // Asso Bastoni, Tre Coppe, Sette Spade
      ActionRequested(player3.id, Action.PlayCard),
      willTick(841993783),
      CardPlayed(player3.id, Card(Quattro, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(485549605),
      CardPlayed(player1.id, Card(Asso, Coppe)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1621593851),
      CardPlayed(player2.id, Card(Asso, Bastoni)),
      mediumDelay,
      TrickCompleted(player2.id),  // 22

      mediumDelay,
      CardsDealt(player2.id, List(Card(Cinque, Denari)), Direction.Player),  // Tre Coppe, Sette Spade, Cinque Denari
      shortDelay,
      CardsDealt(player3.id, List(Card(Sette, Coppe)), Direction.Player),    // Re Bastoni, Re Coppe, Sette Coppe
      shortDelay,
      CardsDealt(player1.id, List(Card(Re, Spade)), Direction.Player),       // Fante Denari, Asso Denari, Re Spade
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1455601775),
      CardPlayed(player2.id, Card(Cinque, Denari)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(734185451),
      CardPlayed(player3.id, Card(Sette, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-1746691187),
      CardPlayed(player1.id, Card(Re, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),  // 4

      mediumDelay,
      CardsDealt(player2.id, List(Card(Sei, Spade)), Direction.Player),      // Tre Coppe, Sette Spade, Sei Spade
      shortDelay,
      CardsDealt(player3.id, List(Card(Quattro, Denari)), Direction.Player), // Re Bastoni, Re Coppe, Quattro Denari
      shortDelay,
      CardsDealt(player1.id, List(Card(Tre, Bastoni)), Direction.Player),    // Fante Denari, Asso Denari, Tre Bastoni
      ActionRequested(player2.id, Action.PlayCard),
      willTick(678473657),
      CardPlayed(player2.id, Card(Sei, Spade)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(408532325),
      CardPlayed(player3.id, Card(Quattro, Denari)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(664044791),
      CardPlayed(player1.id, Card(Fante, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),  // 0

      mediumDelay,
      CardsDealt(player2.id, List(Card(Due, Spade)), Direction.Player),      // Tre Coppe, Sette Spade, Due Spade
      shortDelay,
      CardsDealt(player3.id, List(Card(Cavallo, Coppe)), Direction.Player),  // Re Bastoni, Re Coppe, Cavallo Coppe
      shortDelay,
      CardsDealt(player1.id, List(Card(Sei, Bastoni)), Direction.Player),    // Asso Denari, Tre Bastoni, Sei Bastoni
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1915394923),
      CardPlayed(player2.id, Card(Due, Spade)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(728917085),
      CardPlayed(player3.id, Card(Cavallo, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(226332095),
      CardPlayed(player1.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),  // 3

      ActionRequested(player1.id, Action.PlayCard),
      willTick(-64621599),
      CardPlayed(player1.id, Card(Asso, Denari)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-561177031),
      CardPlayed(player2.id, Card(Sette, Spade)),
      ActionRequested(player3.id, Action.PlayCard),
      willTick(1498365065),
      CardPlayed(player3.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),  // 15

      ActionRequested(player3.id, Action.PlayCard),
      willTick(-304052459),
      CardPlayed(player3.id, Card(Re, Coppe)),
      ActionRequested(player1.id, Action.PlayCard),
      willTick(-361153693),
      CardPlayed(player1.id, Card(Tre, Bastoni)),
      ActionRequested(player2.id, Action.PlayCard),
      willTick(-1889189563),
      CardPlayed(player2.id, Card(Tre, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),  // 24

      longDelay,
      MatchCompleted(
        winnerIds = List(player1.id),
        matchPoints = List(
          PointsCount(List(player1.id), 54),
          PointsCount(List(player2.id), 41),
          PointsCount(List(player3.id), 25),
        ),
        gamePoints = List(
          PointsCount(List(player1.id), player1.points + 1),
          PointsCount(List(player2.id), player2.points),
          PointsCount(List(player3.id), player3.points)
        )
      )
    ).map(_.toMessage(roomId))
