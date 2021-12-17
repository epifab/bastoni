package bastoni.backend

import bastoni.domain.model.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class MatchPlayerSpec extends AnyFreeSpec with Matchers:
  val player1 = Player(PlayerId.newId, "John")
  val player2 = Player(PlayerId.newId, "John")
  val player3 = Player(player1.id, "Jack")

  val gamePlayer: GamePlayer = GamePlayer(player1, 99)
  val matchPlayer = MatchPlayer(gamePlayer, List(Card(Rank.Asso, Suit.Bastoni), Card(Rank.Sette, Suit.Denari)), Nil)

  "Comparing 2 players" in {
    matchPlayer.is(player1) shouldBe true
    matchPlayer.is(player2) shouldBe false
    matchPlayer.is(player3) shouldBe true
    matchPlayer.is(player1.id) shouldBe true
    matchPlayer.is(player2.id) shouldBe false
    matchPlayer.is(player3.id) shouldBe true
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
      MatchPlayer(gamePlayer, List(Card(Rank.Sette, Suit.Denari)), Nil),
      Card(Rank.Asso, Suit.Bastoni)
    )
  }
