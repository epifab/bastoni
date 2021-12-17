package bastoni.backend

import bastoni.domain.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class MatchPlayerSpec extends AnyFreeSpec with Matchers:
  val player1 = Player(PlayerId.newId, "John")
  val player2 = Player(PlayerId.newId, "John")
  val player3 = Player(player1.id, "Jack")

  val gamePlayer: GamePlayer = GamePlayer(player1, 99)
  val matchPlayer = MatchPlayer(gamePlayer, Set(Card(Rank.Asso, Suit.Bastoni), Card(Rank.Sette, Suit.Denari)), Set.empty)

  "Comparing 2 players" in {
    matchPlayer.is(player1) shouldBe true
    matchPlayer.is(player2) shouldBe false
    matchPlayer.is(player3) shouldBe true
  }

  "Player.has" in {
    matchPlayer.has(Card(Rank.Asso, Suit.Bastoni)) shouldBe true
    matchPlayer.has(Card(Rank.Asso, Suit.Denari)) shouldBe false
  }

  "Player.draw" in {
    val card = Card(Rank.Sei, Suit.Spade)
    matchPlayer.draw(card).has(card) shouldBe true
  }

  "Player.play" in {
    matchPlayer.play(Card(Rank.Asso, Suit.Bastoni)) shouldBe (
      MatchPlayer(gamePlayer, Set(Card(Rank.Sette, Suit.Denari)), Set.empty),
      Card(Rank.Asso, Suit.Bastoni)
    )
  }
