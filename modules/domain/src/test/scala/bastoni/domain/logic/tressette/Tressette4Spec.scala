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

  val roomId = RoomId.newId
  val room = Room.cosy(roomId, player1, player2, player3, player4)

  val drawCard      = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  "A game can be played" in {
    val input =
      (
        fs2.Stream(ShuffleDeck(shuffleSeed)) ++
        fs2.Stream(drawCard).repeatN(40) ++
        fs2.Stream(
          PlayCard(player1.id, Card(Sei, Denari)),
          PlayCard(player2.id, Card(Re, Denari)),
          PlayCard(player3.id, Card(Sette, Denari)),
          PlayCard(player4.id, Card(Tre, Denari)),
          completeTrick,

          PlayCard(player4.id, Card(Sei, Spade)),
          PlayCard(player1.id, Card(Cinque, Spade)),
          PlayCard(player2.id, Card(Asso, Spade)),
          PlayCard(player3.id, Card(Due, Spade)),
          completeTrick,

          PlayCard(player3.id, Card(Tre, Spade)),
          PlayCard(player4.id, Card(Quattro, Spade)),
          PlayCard(player1.id, Card(Asso, Coppe)),
          PlayCard(player2.id, Card(Sei, Coppe)),
          completeTrick,

          PlayCard(player3.id, Card(Re, Spade)),
          PlayCard(player4.id, Card(Sette, Spade)),
          PlayCard(player1.id, Card(Re, Bastoni)),
          PlayCard(player2.id, Card(Quattro, Coppe)),
          completeTrick,

          PlayCard(player3.id, Card(Cavallo, Spade)),
          PlayCard(player4.id, Card(Fante, Spade)),
          PlayCard(player1.id, Card(Cinque, Denari)),
          PlayCard(player2.id, Card(Sei, Bastoni)),
          completeTrick,

          PlayCard(player3.id, Card(Cavallo, Denari)),
          PlayCard(player4.id, Card(Quattro, Bastoni)),
          PlayCard(player1.id, Card(Quattro, Denari)),
          PlayCard(player2.id, Card(Fante, Denari)),
          completeTrick,

          PlayCard(player3.id, Card(Due, Denari)),
          PlayCard(player4.id, Card(Fante, Coppe)),
          PlayCard(player1.id, Card(Re, Coppe)),
          PlayCard(player2.id, Card(Sette, Coppe)),
          completeTrick,

          PlayCard(player3.id, Card(Asso, Denari)),
          PlayCard(player4.id, Card(Cavallo, Bastoni)),
          PlayCard(player1.id, Card(Cinque, Bastoni)),
          PlayCard(player2.id, Card(Sette, Bastoni)),
          completeTrick,

          PlayCard(player3.id, Card(Tre, Coppe)),
          PlayCard(player4.id, Card(Cavallo, Coppe)),
          PlayCard(player1.id, Card(Due, Coppe)),
          PlayCard(player2.id, Card(Fante, Bastoni)),
          completeTrick,

          PlayCard(player3.id, Card(Cinque, Coppe)),
          PlayCard(player4.id, Card(Asso, Bastoni)),
          PlayCard(player1.id, Card(Due, Bastoni)),
          PlayCard(player2.id, Card(Tre, Bastoni)),
          completeTrick,
          completeMatch
        )

      ).map(Message(messageId, roomId, _))

    Game.playMatch[cats.Id](room, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
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
      CardDealt(player1.id, Card(Cinque, Spade), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Tre, Spade), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Tre, Denari), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Asso, Coppe), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Due, Denari), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Fante, Spade), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Re, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Tre, Coppe), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Fante, Coppe), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Coppe), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Cavallo, Denari), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Cavallo, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Due, Coppe), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Denari), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Cavallo, Spade), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Quattro, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Re, Coppe), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Coppe), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Asso, Denari), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Sette, Spade), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Denari), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Coppe), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Re, Spade), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Sei, Spade), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Quattro, Denari), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Tre, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player3.id, Card(Due, Spade), Face.Player),
      shortDelay,
      CardDealt(player4.id, Card(Cavallo, Coppe), Face.Player),

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

      ActionRequested(player1.id, Action.PlayCard),
      CardPlayed(player1.id, Card(Sei, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Re, Denari)),
      ActionRequested(player3.id, Action.PlayCardOf(Denari)),
      CardPlayed(player3.id, Card(Sette, Denari)),
      ActionRequested(player4.id, Action.PlayCardOf(Denari)),
      CardPlayed(player4.id, Card(Tre, Denari)),
      mediumDelay,
      TrickCompleted(player4.id),   // 2/3

      ActionRequested(player4.id, Action.PlayCard),
      CardPlayed(player4.id, Card(Sei, Spade)),
      ActionRequested(player1.id, Action.PlayCardOf(Spade)),
      CardPlayed(player1.id, Card(Cinque, Spade)),
      ActionRequested(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Asso, Spade)),
      ActionRequested(player3.id, Action.PlayCardOf(Spade)),
      CardPlayed(player3.id, Card(Due, Spade)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Tre, Spade)),
      ActionRequested(player4.id, Action.PlayCardOf(Spade)),
      CardPlayed(player4.id, Card(Quattro, Spade)),
      ActionRequested(player1.id, Action.PlayCardOf(Spade)),
      CardPlayed(player1.id, Card(Asso, Coppe)),
      ActionRequested(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Re, Spade)),
      ActionRequested(player4.id, Action.PlayCardOf(Spade)),
      CardPlayed(player4.id, Card(Sette, Spade)),
      ActionRequested(player1.id, Action.PlayCardOf(Spade)),
      CardPlayed(player1.id, Card(Re, Bastoni)),
      ActionRequested(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Quattro, Coppe)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Cavallo, Spade)),
      ActionRequested(player4.id, Action.PlayCardOf(Spade)),
      CardPlayed(player4.id, Card(Fante, Spade)),
      ActionRequested(player1.id, Action.PlayCardOf(Spade)),
      CardPlayed(player1.id, Card(Cinque, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Sei, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Cavallo, Denari)),
      ActionRequested(player4.id, Action.PlayCardOf(Denari)),
      CardPlayed(player4.id, Card(Quattro, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Denari)),
      CardPlayed(player1.id, Card(Quattro, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Fante, Denari)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Due, Denari)),
      ActionRequested(player4.id, Action.PlayCardOf(Denari)),
      CardPlayed(player4.id, Card(Fante, Coppe)),
      ActionRequested(player1.id, Action.PlayCardOf(Denari)),
      CardPlayed(player1.id, Card(Re, Coppe)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Coppe)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Asso, Denari)),
      ActionRequested(player4.id, Action.PlayCardOf(Denari)),
      CardPlayed(player4.id, Card(Cavallo, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Denari)),
      CardPlayed(player1.id, Card(Cinque, Bastoni)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Tre, Coppe)),
      ActionRequested(player4.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player4.id, Card(Cavallo, Coppe)),
      ActionRequested(player1.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player1.id, Card(Due, Coppe)),
      ActionRequested(player2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player2.id, Card(Fante, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),

      ActionRequested(player3.id, Action.PlayCard),
      CardPlayed(player3.id, Card(Cinque, Coppe)),
      ActionRequested(player4.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player4.id, Card(Asso, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      ActionRequested(player2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player2.id, Card(Tre, Bastoni)),
      mediumDelay,
      TrickCompleted(player3.id),

      longDelay,
      MatchCompleted(
        winnerIds = List(player3.id, player1.id),
        matchPoints = List(
          PointsCount(List(player3.id, player1.id), 11),
          PointsCount(List(player4.id, player2.id), 0)
        ),
        gamePoints = List(
          PointsCount(List(player3.id, player1.id), 11),
          PointsCount(List(player4.id, player2.id), 0)
        )
      )
    ).map(_.toMessage(roomId))
  }
