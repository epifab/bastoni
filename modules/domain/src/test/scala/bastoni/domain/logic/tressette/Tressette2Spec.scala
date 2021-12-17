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

class Tressette2Spec extends AnyFreeSpec with Matchers:
  import Fixtures.*

  val roomId = RoomId.newId
  val players = List(player1, player2)

  val drawCard      = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  "A game can be played" in {
    val input =
      (
        fs2.Stream(ShuffleDeck(shuffleSeed)) ++
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

      ).map(Message(messageId, roomId, _))

    Game.playMatch[cats.Id](roomId, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Spade), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Sette, Denari), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Spade), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Sei, Denari), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Re, Denari), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Coppe), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Asso, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Cinque, Spade), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Tre, Spade), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Tre, Denari), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Asso, Coppe), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Due, Denari), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Spade), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Re, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Bastoni), Face.Player),
      shortDelay,
      CardDealt(player1.id, Card(Tre, Coppe), Face.Player),
      shortDelay,
      CardDealt(player2.id, Card(Fante, Coppe), Face.Player),
      ActionRequested(player1.id, Action.PlayCard),

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
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),   // 1/3
      mediumDelay,

      CardDealt(player2.id, Card(Cinque, Bastoni), Face.Up),
      shortDelay,
      CardDealt(player1.id, Card(Sei, Coppe), Face.Up),
      ActionRequested(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Sei, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Cavallo, Denari), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Cavallo, Bastoni), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Sette, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Tre, Denari)),
      mediumDelay,
      TrickCompleted(player2.id),   // 2/3
      mediumDelay,

      CardDealt(player2.id, Card(Due, Coppe), Face.Up),
      shortDelay,
      CardDealt(player1.id, Card(Fante, Denari), Face.Up),
      ActionRequested(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Cinque, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Due, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2/3
      mediumDelay,

      CardDealt(player1.id, Card(Cavallo, Spade), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Bastoni), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Cavallo, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 1
      mediumDelay,

      CardDealt(player1.id, Card(Re, Coppe), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Quattro, Coppe), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Fante, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Quattro, Bastoni)),
      mediumDelay,
      TrickCompleted(player1.id),   // 1 + 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Asso, Denari), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Spade), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Asso, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Cinque, Denari), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Sette, Coppe), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Cinque, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sette, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 1/3
      mediumDelay,

      CardDealt(player1.id, Card(Re, Spade), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Sei, Spade), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Tre, Coppe)),
      ActionRequested(player2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player2.id, Card(Quattro, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 2/3
      mediumDelay,

      CardDealt(player1.id, Card(Quattro, Denari), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Tre, Bastoni), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Quattro, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Quattro, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 2 + 2/3
      mediumDelay,

      CardDealt(player1.id, Card(Due, Spade), Face.Up),
      shortDelay,
      CardDealt(player2.id, Card(Cavallo, Coppe), Face.Up),
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Due, Denari)),
      ActionRequested(player2.id, Action.PlayCardOf(Denari)),
      CardPlayed(player2.id, Card(Sei, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 3
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Due, Spade)),
      ActionRequested(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Asso, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 4 + 1/3
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Re, Spade)),
      ActionRequested(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Fante, Spade)),
      mediumDelay,
      TrickCompleted(player1.id),   // 4 + 2/3
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Tre, Spade)),
      ActionRequested(player2.id, Action.PlayCardOf(Spade)),
      CardPlayed(player2.id, Card(Fante, Coppe)),
      mediumDelay,
      TrickCompleted(player1.id),   // 5 + 1/3
      ActionRequested(player1.id, Action.PlayCard),

      CardPlayed(player1.id, Card(Cinque, Coppe)),
      ActionRequested(player2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player2.id, Card(Cavallo, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 1
      ActionRequested(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Tre, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),   // 1 + 1/3
      ActionRequested(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Asso, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 2 + 1/3
      ActionRequested(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Cavallo, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 3
      ActionRequested(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Fante, Bastoni)),
      ActionRequested(player1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(player1.id, Card(Cavallo, Spade)),
      mediumDelay,
      TrickCompleted(player2.id),   // 3 + 2/3
      ActionRequested(player2.id, Action.PlayCard),

      CardPlayed(player2.id, Card(Due, Coppe)),
      ActionRequested(player1.id, Action.PlayCardOf(Coppe)),
      CardPlayed(player1.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(player2.id),   // 5

      longDelay,
      MatchCompleted(
        winnerIds = List(player2.id),
        matchPoints = List(
          PointsCount(List(player2.id), 6),
          PointsCount(List(player1.id), 5)
        ),
        gamePoints = List(
          PointsCount(List(player2.id), 6),
          PointsCount(List(player1.id), 5)
        )
      )
    ).map(_.toMessage(roomId))
  }

  "Game is aborted if one of the active players leaves" in {
    val input = fs2.Stream[fs2.Pure, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      drawCard,
      PlayerLeftTable(player1, 1),
      drawCard, // too late, game was aborted
    ).map(_.toMessage(roomId))

    Game.playMatch[cats.Id](roomId, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni), Face.Player),
      shortDelay,
      MatchAborted
    ).map(_.toMessage(roomId))
  }

  "Game continues if another player joins and leaves" in {
    val input = fs2.Stream[fs2.Pure, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      PlayerJoinedTable(player3, 3),
      PlayerLeftTable(player3, 3),
      drawCard,
    ).map(_.toMessage(roomId))

    Game.playMatch[cats.Id](roomId, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardDealt(player1.id, Card(Due, Bastoni), Face.Player),
      shortDelay
    ).map(_.toMessage(roomId))
  }
