package bastoni.domain.logic
package tressette

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import cats.catsInstancesForId
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Tressette4Spec extends AnyFreeSpec with Matchers:
  import Fixtures.*

  val players = List(user1, user2, user3, user4)

  val drawCards      = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  "A game can be played" ignore {
    val input =
      (
        fs2.Stream(ShuffleDeck(shuffleSeed)) ++
        fs2.Stream(drawCards).repeatN(40) ++
        fs2.Stream(
          PlayCard(user1.id, Card(Sei, Denari)),
          PlayCard(user2.id, Card(Re, Denari)),
          PlayCard(user3.id, Card(Sette, Denari)),
          PlayCard(user4.id, Card(Tre, Denari)),
          completeTrick,

          PlayCard(user4.id, Card(Sei, Spade)),
          PlayCard(user1.id, Card(Cinque, Spade)),
          PlayCard(user2.id, Card(Asso, Spade)),
          PlayCard(user3.id, Card(Due, Spade)),
          completeTrick,

          PlayCard(user3.id, Card(Tre, Spade)),
          PlayCard(user4.id, Card(Quattro, Spade)),
          PlayCard(user1.id, Card(Asso, Coppe)),
          PlayCard(user2.id, Card(Sei, Coppe)),
          completeTrick,

          PlayCard(user3.id, Card(Re, Spade)),
          PlayCard(user4.id, Card(Sette, Spade)),
          PlayCard(user1.id, Card(Re, Bastoni)),
          PlayCard(user2.id, Card(Quattro, Coppe)),
          completeTrick,

          PlayCard(user3.id, Card(Cavallo, Spade)),
          PlayCard(user4.id, Card(Fante, Spade)),
          PlayCard(user1.id, Card(Cinque, Denari)),
          PlayCard(user2.id, Card(Sei, Bastoni)),
          completeTrick,

          PlayCard(user3.id, Card(Cavallo, Denari)),
          PlayCard(user4.id, Card(Quattro, Bastoni)),
          PlayCard(user1.id, Card(Quattro, Denari)),
          PlayCard(user2.id, Card(Fante, Denari)),
          completeTrick,

          PlayCard(user3.id, Card(Due, Denari)),
          PlayCard(user4.id, Card(Fante, Coppe)),
          PlayCard(user1.id, Card(Re, Coppe)),
          PlayCard(user2.id, Card(Sette, Coppe)),
          completeTrick,

          PlayCard(user3.id, Card(Asso, Denari)),
          PlayCard(user4.id, Card(Cavallo, Bastoni)),
          PlayCard(user1.id, Card(Cinque, Bastoni)),
          PlayCard(user2.id, Card(Sette, Bastoni)),
          completeTrick,

          PlayCard(user3.id, Card(Tre, Coppe)),
          PlayCard(user4.id, Card(Cavallo, Coppe)),
          PlayCard(user1.id, Card(Due, Coppe)),
          PlayCard(user2.id, Card(Fante, Bastoni)),
          completeTrick,

          PlayCard(user3.id, Card(Cinque, Coppe)),
          PlayCard(user4.id, Card(Asso, Bastoni)),
          PlayCard(user1.id, Card(Due, Bastoni)),
          PlayCard(user2.id, Card(Tre, Bastoni)),
          completeTrick,
          completeMatch
        )

      ).map(Message(messageId, room1, _))

    Game.playGame[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(user1.id, List(Card(Due, Bastoni), Card(Asso, Spade), Card(Sette, Denari), Card(Quattro, Spade), Card(Sei, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(user2.id, List(Card(Re, Denari), Card(Cinque, Coppe), Card(Asso, Bastoni), Card(Cinque, Spade), Card(Sei, Bastoni)), Direction.Player),
      shortDelay,
      CardsDealt(user3.id, List(Card(Tre, Spade), Card(Tre, Denari), Card(Asso, Coppe), Card(Fante, Bastoni), Card(Due, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(user4.id, List(Card(Fante, Spade), Card(Re, Bastoni), Card(Sette, Bastoni), Card(Tre, Coppe), Card(Fante, Coppe)), Direction.Player),
      shortDelay,
      CardsDealt(user1.id, List(Card(Cinque, Bastoni), Card(Sei, Coppe), Card(Cavallo, Denari), Card(Cavallo, Bastoni), Card(Due, Coppe)), Direction.Player),
      shortDelay,
      CardsDealt(user2.id, List(Card(Fante, Denari), Card(Cavallo, Spade), Card(Quattro, Bastoni), Card(Re, Coppe), Card(Quattro, Coppe)), Direction.Player),
      shortDelay,
      CardsDealt(user3.id, List(Card(Asso, Denari), Card(Sette, Spade), Card(Cinque, Denari), Card(Sette, Coppe), Card(Re, Spade)), Direction.Player),
      shortDelay,
      CardsDealt(user4.id, List(Card(Sei, Spade), Card(Quattro, Denari), Card(Tre, Bastoni), Card(Due, Spade), Card(Cavallo, Coppe)), Direction.Player),

      // **************************
      // player1
      //   Sei, Denari
      //   Cinque, Denari
      //   Quattro, Denari

      //   Due, Coppe
      //   Asso, Coppe
      //   Re, Coppe

      //   Cinque, Spade

      //   Due, Bastoni
      //   Re, Bastoni
      //   Cinque, Bastoni

      // **************************
      // player2
      //   Re, Denari
      //   Fante, Denari

      //   Sette, Coppe
      //   Sei, Coppe
      //   Quattro, Coppe

      //   Asso, Spade

      //   Tre, Bastoni
      //   Fante, Bastoni
      //   Sette, Bastoni
      //   Sei, Bastoni

      // **************************
      // player3
      //   Due, Denari
      //   Asso, Denari
      //   Cavallo, Denari
      //   Sette, Denari

      //   Tre, Coppe
      //   Cinque, Coppe

      //   Tre, Spade
      //   Due, Spade
      //   Re, Spade
      //   Cavallo, Spade

      // **************************
      // player4
      //   Tre, Denari

      //   Cavallo, Coppe
      //   Fante, Coppe

      //   Fante, Spade
      //   Sette, Spade
      //   Sei, Spade
      //   Quattro, Spade

      //   Asso, Bastoni
      //   Cavallo, Bastoni
      //   Quattro, Bastoni

      ActionRequested(user1.id, Action.PlayCard),
      CardPlayed(user1.id, Card(Sei, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Re, Denari)),
      ActionRequested(user3.id, Action.PlayCardOf(Denari)),
      CardPlayed(user3.id, Card(Sette, Denari)),
      ActionRequested(user4.id, Action.PlayCardOf(Denari)),
      CardPlayed(user4.id, Card(Tre, Denari)),
      mediumDelay,
      TrickCompleted(user4.id),   // 2/3

      ActionRequested(user4.id, Action.PlayCard),
      CardPlayed(user4.id, Card(Sei, Spade)),
      ActionRequested(user1.id, Action.PlayCardOf(Spade)),
      CardPlayed(user1.id, Card(Cinque, Spade)),
      ActionRequested(user2.id, Action.PlayCardOf(Spade)),
      CardPlayed(user2.id, Card(Asso, Spade)),
      ActionRequested(user3.id, Action.PlayCardOf(Spade)),
      CardPlayed(user3.id, Card(Due, Spade)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Tre, Spade)),
      ActionRequested(user4.id, Action.PlayCardOf(Spade)),
      CardPlayed(user4.id, Card(Quattro, Spade)),
      ActionRequested(user1.id, Action.PlayCardOf(Spade)),
      CardPlayed(user1.id, Card(Asso, Coppe)),
      ActionRequested(user2.id, Action.PlayCardOf(Spade)),
      CardPlayed(user2.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Re, Spade)),
      ActionRequested(user4.id, Action.PlayCardOf(Spade)),
      CardPlayed(user4.id, Card(Sette, Spade)),
      ActionRequested(user1.id, Action.PlayCardOf(Spade)),
      CardPlayed(user1.id, Card(Re, Bastoni)),
      ActionRequested(user2.id, Action.PlayCardOf(Spade)),
      CardPlayed(user2.id, Card(Quattro, Coppe)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Cavallo, Spade)),
      ActionRequested(user4.id, Action.PlayCardOf(Spade)),
      CardPlayed(user4.id, Card(Fante, Spade)),
      ActionRequested(user1.id, Action.PlayCardOf(Spade)),
      CardPlayed(user1.id, Card(Cinque, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Spade)),
      CardPlayed(user2.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Cavallo, Denari)),
      ActionRequested(user4.id, Action.PlayCardOf(Denari)),
      CardPlayed(user4.id, Card(Quattro, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Denari)),
      CardPlayed(user1.id, Card(Quattro, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Fante, Denari)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Due, Denari)),
      ActionRequested(user4.id, Action.PlayCardOf(Denari)),
      CardPlayed(user4.id, Card(Fante, Coppe)),
      ActionRequested(user1.id, Action.PlayCardOf(Denari)),
      CardPlayed(user1.id, Card(Re, Coppe)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Sette, Coppe)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Asso, Denari)),
      ActionRequested(user4.id, Action.PlayCardOf(Denari)),
      CardPlayed(user4.id, Card(Cavallo, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Denari)),
      CardPlayed(user1.id, Card(Cinque, Bastoni)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Tre, Coppe)),
      ActionRequested(user4.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user4.id, Card(Cavallo, Coppe)),
      ActionRequested(user1.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user1.id, Card(Due, Coppe)),
      ActionRequested(user2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user2.id, Card(Fante, Bastoni)),
      mediumDelay,
      TrickCompleted(user3.id),

      ActionRequested(user3.id, Action.PlayCard),
      CardPlayed(user3.id, Card(Cinque, Coppe)),
      ActionRequested(user4.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user4.id, Card(Asso, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user1.id, Card(Due, Bastoni)),
      ActionRequested(user2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user2.id, Card(Tre, Bastoni)),
      mediumDelay,
      TrickCompleted(user3.id),

      longDelay,
      GameCompleted(
        winnerIds = List(user3.id, user1.id),
        points = List(
          PointsCount(List(user3.id, user1.id), 11),
          PointsCount(List(user4.id, user2.id), 0)
        ),
        matchPoints = List(
          PointsCount(List(user3.id, user1.id), 11),
          PointsCount(List(user4.id, user2.id), 0)
        )
      )
    ).map(_.toMessage(room1))
  }
