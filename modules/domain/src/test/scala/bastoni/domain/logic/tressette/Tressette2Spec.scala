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

  val players = List(user1, user2)

  val drawCards      = Continue
  val completeTrick = Continue
  val completeMatch = Continue

  "A game can be played" ignore {
    val input =
      (
        fs2.Stream(ShuffleDeck(shuffleSeed)) ++
        fs2.Stream(drawCards).repeatN(20) ++
        fs2.Stream(
          PlayCard(user1.id, Card(Sei, Denari)),
          PlayCard(user2.id, Card(Re, Denari)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user2.id, Card(Sei, Bastoni)),
          PlayCard(user1.id, Card(Re, Bastoni)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Sette, Denari)),
          PlayCard(user2.id, Card(Tre, Denari)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user2.id, Card(Cinque, Bastoni)),
          PlayCard(user1.id, Card(Due, Bastoni)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Cavallo, Denari)),
          PlayCard(user2.id, Card(Sette, Bastoni)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Fante, Denari)),
          PlayCard(user2.id, Card(Quattro, Bastoni)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Asso, Denari)),
          PlayCard(user2.id, Card(Sette, Spade)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Cinque, Denari)),
          PlayCard(user2.id, Card(Sette, Coppe)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Tre, Coppe)),
          PlayCard(user2.id, Card(Quattro, Coppe)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Quattro, Denari)),
          PlayCard(user2.id, Card(Quattro, Spade)),
          completeTrick,

          drawCards,
          drawCards,
          PlayCard(user1.id, Card(Due, Denari)),
          PlayCard(user2.id, Card(Sei, Spade)),
          completeTrick,

          PlayCard(user1.id, Card(Due, Spade)),
          PlayCard(user2.id, Card(Asso, Spade)),
          completeTrick,

          PlayCard(user1.id, Card(Re, Spade)),
          PlayCard(user2.id, Card(Fante, Spade)),
          completeTrick,

          PlayCard(user1.id, Card(Tre, Spade)),
          PlayCard(user2.id, Card(Fante, Coppe)),
          completeTrick,

          PlayCard(user1.id, Card(Cinque, Coppe)),
          PlayCard(user2.id, Card(Cavallo, Coppe)),
          completeTrick,

          PlayCard(user2.id, Card(Tre, Bastoni)),
          PlayCard(user1.id, Card(Cinque, Spade)),
          completeTrick,

          PlayCard(user2.id, Card(Asso, Bastoni)),
          PlayCard(user1.id, Card(Sei, Coppe)),
          completeTrick,

          PlayCard(user2.id, Card(Cavallo, Bastoni)),
          PlayCard(user1.id, Card(Re, Coppe)),
          completeTrick,

          PlayCard(user2.id, Card(Fante, Bastoni)),
          PlayCard(user1.id, Card(Cavallo, Spade)),
          completeTrick,

          PlayCard(user2.id, Card(Due, Coppe)),
          PlayCard(user1.id, Card(Asso, Coppe)),
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
      CardsDealt(user1.id, List(Card(Tre, Spade), Card(Tre, Denari), Card(Asso, Coppe), Card(Fante, Bastoni), Card(Due, Denari)), Direction.Player),
      shortDelay,
      CardsDealt(user2.id, List(Card(Fante, Spade), Card(Re, Bastoni), Card(Sette, Bastoni), Card(Tre, Coppe), Card(Fante, Coppe)), Direction.Player),
      ActionRequested(user1.id, Action.PlayCard),

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

      CardPlayed(user1.id, Card(Sei, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Re, Denari)),
      mediumDelay,
      TrickCompleted(user2.id),   // 1/3
      mediumDelay,

      CardsDealt(user2.id, List(Card(Cinque, Bastoni)), Direction.Up),
      shortDelay,
      CardsDealt(user1.id, List(Card(Sei, Coppe)), Direction.Up),
      ActionRequested(user2.id, Action.PlayCard),

      CardPlayed(user2.id, Card(Sei, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(user1.id, Card(Re, Bastoni)),
      mediumDelay,
      TrickCompleted(user1.id),   // 1/3
      mediumDelay,

      CardsDealt(user1.id, List(Card(Cavallo, Denari)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Cavallo, Bastoni)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Sette, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Tre, Denari)),
      mediumDelay,
      TrickCompleted(user2.id),   // 2/3
      mediumDelay,

      CardsDealt(user2.id, List(Card(Due, Coppe)), Direction.Up),
      shortDelay,
      CardsDealt(user1.id, List(Card(Fante, Denari)), Direction.Up),
      ActionRequested(user2.id, Action.PlayCard),

      CardPlayed(user2.id, Card(Cinque, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(user1.id, Card(Due, Bastoni)),
      mediumDelay,
      TrickCompleted(user1.id),   // 2/3
      mediumDelay,

      CardsDealt(user1.id, List(Card(Cavallo, Spade)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Quattro, Bastoni)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Cavallo, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Sette, Bastoni)),
      mediumDelay,
      TrickCompleted(user1.id),   // 1
      mediumDelay,

      CardsDealt(user1.id, List(Card(Re, Coppe)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Quattro, Coppe)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Fante, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Quattro, Bastoni)),
      mediumDelay,
      TrickCompleted(user1.id),   // 1 + 1/3
      mediumDelay,

      CardsDealt(user1.id, List(Card(Asso, Denari)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Sette, Spade)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Asso, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Sette, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),   // 2 + 1/3
      mediumDelay,

      CardsDealt(user1.id, List(Card(Cinque, Denari)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Sette, Coppe)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Cinque, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Sette, Coppe)),
      mediumDelay,
      TrickCompleted(user1.id),   // 2 + 1/3
      mediumDelay,

      CardsDealt(user1.id, List(Card(Re, Spade)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Sei, Spade)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Tre, Coppe)),
      ActionRequested(user2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user2.id, Card(Quattro, Coppe)),
      mediumDelay,
      TrickCompleted(user1.id),   // 2 + 2/3
      mediumDelay,

      CardsDealt(user1.id, List(Card(Quattro, Denari)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Tre, Bastoni)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Quattro, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Quattro, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),   // 2 + 2/3
      mediumDelay,

      CardsDealt(user1.id, List(Card(Due, Spade)), Direction.Up),
      shortDelay,
      CardsDealt(user2.id, List(Card(Cavallo, Coppe)), Direction.Up),
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Due, Denari)),
      ActionRequested(user2.id, Action.PlayCardOf(Denari)),
      CardPlayed(user2.id, Card(Sei, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),   // 3
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Due, Spade)),
      ActionRequested(user2.id, Action.PlayCardOf(Spade)),
      CardPlayed(user2.id, Card(Asso, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),   // 4 + 1/3
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Re, Spade)),
      ActionRequested(user2.id, Action.PlayCardOf(Spade)),
      CardPlayed(user2.id, Card(Fante, Spade)),
      mediumDelay,
      TrickCompleted(user1.id),   // 4 + 2/3
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Tre, Spade)),
      ActionRequested(user2.id, Action.PlayCardOf(Spade)),
      CardPlayed(user2.id, Card(Fante, Coppe)),
      mediumDelay,
      TrickCompleted(user1.id),   // 5 + 1/3
      ActionRequested(user1.id, Action.PlayCard),

      CardPlayed(user1.id, Card(Cinque, Coppe)),
      ActionRequested(user2.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user2.id, Card(Cavallo, Coppe)),
      mediumDelay,
      TrickCompleted(user2.id),   // 1
      ActionRequested(user2.id, Action.PlayCard),

      CardPlayed(user2.id, Card(Tre, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(user1.id, Card(Cinque, Spade)),
      mediumDelay,
      TrickCompleted(user2.id),   // 1 + 1/3
      ActionRequested(user2.id, Action.PlayCard),

      CardPlayed(user2.id, Card(Asso, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(user1.id, Card(Sei, Coppe)),
      mediumDelay,
      TrickCompleted(user2.id),   // 2 + 1/3
      ActionRequested(user2.id, Action.PlayCard),

      CardPlayed(user2.id, Card(Cavallo, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(user1.id, Card(Re, Coppe)),
      mediumDelay,
      TrickCompleted(user2.id),   // 3
      ActionRequested(user2.id, Action.PlayCard),

      CardPlayed(user2.id, Card(Fante, Bastoni)),
      ActionRequested(user1.id, Action.PlayCardOf(Bastoni)),
      CardPlayed(user1.id, Card(Cavallo, Spade)),
      mediumDelay,
      TrickCompleted(user2.id),   // 3 + 2/3
      ActionRequested(user2.id, Action.PlayCard),

      CardPlayed(user2.id, Card(Due, Coppe)),
      ActionRequested(user1.id, Action.PlayCardOf(Coppe)),
      CardPlayed(user1.id, Card(Asso, Coppe)),
      mediumDelay,
      TrickCompleted(user2.id),   // 5

      longDelay,
      GameCompleted(
        winnerIds = List(user2.id),
        points = List(
          PointsCount(List(user2.id), 6),
          PointsCount(List(user1.id), 5)
        ),
        matchPoints = List(
          PointsCount(List(user2.id), 6),
          PointsCount(List(user1.id), 5)
        )
      )
    ).map(_.toMessage(room1))
  }

  "Game is aborted if one of the active players leaves" ignore {
    val input = fs2.Stream[fs2.Pure, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      drawCards,
      PlayerLeftTable(user1, 1),
      drawCards, // too late, game was aborted
    ).map(_.toMessage(room1))

    Game.playGame[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(user1.id, List(Card(Due, Bastoni)), Direction.Player),
      shortDelay,
      GameAborted
    ).map(_.toMessage(room1))
  }

  "Game continues if another player joins and leaves" ignore {
    val input = fs2.Stream[fs2.Pure, ServerEvent | Command](
      ShuffleDeck(shuffleSeed),
      PlayerJoinedTable(user3, 3),
      PlayerLeftTable(user3, 3),
      drawCards,
    ).map(_.toMessage(room1))

    Game.playGame[cats.Id](room1, players, messageId)(input).compile.toList shouldBe List[ServerEvent | Command | Delayed[Command]](
      DeckShuffled(shuffledDeck),
      mediumDelay,
      CardsDealt(user1.id, List(Card(Due, Bastoni)), Direction.Player),
      shortDelay
    ).map(_.toMessage(room1))
  }
