package bastoni.backend
package tressette

import bastoni.backend.DelayedMessage
import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.model.Rank.{Tre, *}
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class Tressette2Spec extends AnyFreeSpec with Matchers:
  val player1 = Player(PlayerId.newId, "Tizio")
  val player2 = Player(PlayerId.newId, "Caio")

  val roomId = RoomId.newId
  val room = Room(roomId, List(player1, player2))

  val drawCard      = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  val shortDelay = DelayedCommand(Continue, Delay.Short)
  val mediumDelay = DelayedCommand(Continue, Delay.Medium)
  val longDelay = DelayedCommand(Continue, Delay.Long)

  "A game can be played" in {
    val input =
      (
        fs2.Stream(ShuffleDeck(10)) ++
        fs2.Stream(drawCard).repeatN(20) ++
        fs2.Stream(
          PlayCard(player1.id, Card(Sei, Denari)),
          PlayCard(player2.id, Card(Re, Denari)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player2.id, Card(Sei, Bastoni)),
          PlayCard(player1.id, Card(Re, Bastoni)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Sette, Denari)),
          PlayCard(player2.id, Card(Tre, Denari)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player2.id, Card(Cinque, Bastoni)),
          PlayCard(player1.id, Card(Due, Bastoni)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Cavallo, Denari)),
          PlayCard(player2.id, Card(Sette, Bastoni)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Fante, Denari)),
          PlayCard(player2.id, Card(Quattro, Bastoni)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Asso, Denari)),
          PlayCard(player2.id, Card(Sette, Spade)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Cinque, Denari)),
          PlayCard(player2.id, Card(Sette, Coppe)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Tre, Coppe)),
          PlayCard(player2.id, Card(Quattro, Coppe)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Quattro, Denari)),
          PlayCard(player2.id, Card(Quattro, Spade)),
          completeTrick,

          drawCard,
          drawCard,
          PlayCard(player1.id, Card(Due, Denari)),
          PlayCard(player2.id, Card(Sei, Spade)),
          completeTrick,

          PlayCard(player1.id, Card(Due, Spade)),
          PlayCard(player2.id, Card(Asso, Spade)),
          completeTrick,

          PlayCard(player1.id, Card(Re, Spade)),
          PlayCard(player2.id, Card(Fante, Spade)),
          completeTrick,

          PlayCard(player1.id, Card(Tre, Spade)),
          PlayCard(player2.id, Card(Fante, Coppe)),
          completeTrick,

          PlayCard(player1.id, Card(Cinque, Coppe)),
          PlayCard(player2.id, Card(Cavallo, Coppe)),
          completeTrick,

          PlayCard(player2.id, Card(Tre, Bastoni)),
          PlayCard(player1.id, Card(Cinque, Spade)),
          completeTrick,

          PlayCard(player2.id, Card(Asso, Bastoni)),
          PlayCard(player1.id, Card(Sei, Coppe)),
          completeTrick,

          PlayCard(player2.id, Card(Cavallo, Bastoni)),
          PlayCard(player1.id, Card(Re, Coppe)),
          completeTrick,

          PlayCard(player2.id, Card(Fante, Bastoni)),
          PlayCard(player1.id, Card(Cavallo, Spade)),
          completeTrick,

          PlayCard(player2.id, Card(Due, Coppe)),
          PlayCard(player1.id, Card(Asso, Coppe)),
          completeTrick,

          completeMatch
        )

      ).map(Message(roomId, _))

    Game.playMatch[fs2.Pure](room)(input).compile.toList shouldBe List[Event | Command | DelayedCommand](
      DeckShuffled(10),
      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni)),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade)),
      shortDelay,
      CardDealt(player1.id, Card(Sette, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Spade)),
      shortDelay,
      CardDealt(player1.id, Card(Sei, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Re, Denari)),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Coppe)),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Bastoni)),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Spade)),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Bastoni)),
      shortDelay,
      CardDealt(player1.id, Card(Tre, Spade)),
      shortDelay,
      CardDealt(player2.id, Card(Tre, Denari)),
      shortDelay,
      CardDealt(player1.id, Card(Asso, Coppe)),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Bastoni)),
      shortDelay,
      CardDealt(player1.id, Card(Due, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Spade)),
      shortDelay,
      CardDealt(player1.id, Card(Re, Bastoni)),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Bastoni)),
      shortDelay,
      CardDealt(player1.id, Card(Tre, Coppe)),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),

      // player1
      //  Due, Bastoni
      //  Sette, Denari
      //  Sei, Denari
      //  Cinque, Coppe
      //  Cinque, Spade
      //  Tre, Spade
      //  Asso, Coppe
      //  Due, Denari
      //  Re, Bastoni
      //  Tre, Coppe

      // player2
      //  Asso, Spade
      //  Quattro, Spade
      //  Re, Denari
      //  Asso, Bastoni
      //  Sei, Bastoni
      //  Tre, Denari
      //  Fante, Bastoni
      //  Fante, Spade
      //  Sette, Bastoni
      //  Fante, Coppe

      CardPlayed(player1.id, Card(Sei, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),   // 1/3
      mediumDelay,

      CardDealt(player2.id, Card(Cinque, Bastoni)),
      shortDelay,
      CardDealt(player1.id, Card(Sei, Coppe)),
      ActionRequest(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Sei, Bastoni)),
      ActionRequest(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Cavallo, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Cavallo, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Sette, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Tre, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),   // 2/3
      mediumDelay,

      CardDealt(player2.id, Card(Due, Coppe)),
      shortDelay,
      CardDealt(player1.id, Card(Fante, Denari)),
      ActionRequest(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      ActionRequest(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2/3
      mediumDelay,

      CardDealt(player1.id, Card(Cavallo, Spade)),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Cavallo, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 1
      mediumDelay,

      CardDealt(player1.id, Card(Re, Coppe)),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Fante, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Quattro, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 1 + 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Asso, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Spade)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Asso, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Cinque, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Cinque, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Re, Spade)),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Spade)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Tre, Coppe)),
      ActionRequest(player2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player2.id, Card(Quattro, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 2/3
      mediumDelay,

      CardDealt(player1.id, Card(Quattro, Denari)),
      shortDelay,
      CardDealt(player2.id, Card(Tre, Bastoni)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Quattro, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Quattro, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 2/3
      mediumDelay,

      CardDealt(player1.id, Card(Due, Spade)),
      shortDelay,
      CardDealt(player2.id, Card(Cavallo, Coppe)),
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Due, Denari)),
      ActionRequest(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sei, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 3
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Due, Spade)),
      ActionRequest(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Asso, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 4 + 1/3
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Re, Spade)),
      ActionRequest(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Fante, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 4 + 2/3
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Tre, Spade)),
      ActionRequest(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Fante, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),   // 5 + 1/3
      ActionRequest(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Cinque, Coppe)),
      ActionRequest(player2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player2.id, Card(Cavallo, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 1
      ActionRequest(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Tre, Bastoni)),
      ActionRequest(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),   // 1 + 1/3
      ActionRequest(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Asso, Bastoni)),
      ActionRequest(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 2 + 1/3
      ActionRequest(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Cavallo, Bastoni)),
      ActionRequest(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 3
      ActionRequest(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Fante, Bastoni)),
      ActionRequest(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Cavallo, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),   // 3 + 2/3
      ActionRequest(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Due, Coppe)),
      ActionRequest(player1.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player1.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 5

      longDelay,
      PointsCount(List(player2.id), 6),
      PointsCount(List(player1.id), 5),
      MatchCompleted(List(player2.id))
    ).map(_.toMessage(roomId))
  }
