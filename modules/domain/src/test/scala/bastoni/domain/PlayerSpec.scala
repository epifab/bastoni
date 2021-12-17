package bastoni.backend

import bastoni.domain.model.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class PlayerSpec extends AnyFreeSpec with Matchers:
  val user1 = User(UserId.newId, "John")
  val user2 = User(UserId.newId, "John")
  val user3 = User(user1.id, "Jack")

  val matchPlayer: MatchPlayer = MatchPlayer(user1, 99)
  val player = Player(matchPlayer, List(Card(Rank.Asso, Suit.Bastoni), Card(Rank.Sette, Suit.Denari)), Nil)

  "Comparing 2 players" in {
    player.is(user1) shouldBe true
    player.is(user2) shouldBe false
    player.is(user3) shouldBe true
    player.is(user1.id) shouldBe true
    player.is(user2.id) shouldBe false
    player.is(user3.id) shouldBe true
  }

  "Player.has" in {
    player.has(Card(Rank.Asso, Suit.Bastoni)) shouldBe true
    player.has(Card(Rank.Asso, Suit.Denari)) shouldBe false
  }

  "Player.draw" in {
    val card = Card(Rank.Sei, Suit.Spade)
    player.draw(card).has(card) shouldBe true
  }

  "Player.play" in {
    player.play(Card(Rank.Asso, Suit.Bastoni)) shouldBe Player(matchPlayer, List(Card(Rank.Sette, Suit.Denari)), Nil)
  }
