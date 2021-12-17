package bastoni.backend

import bastoni.domain.model.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class MatchPlayerSpec extends AnyFreeSpec with Matchers:
  val player1 = User(UserId.newId, "John")
  val player2 = User(UserId.newId, "John")
  val player3 = User(player1.id, "Jack")

  val player = MatchPlayer(player1, 99)

  "Comparing 2 players" in {
    player.is(player1) shouldBe true
    player.is(player2) shouldBe false
    player.is(player3) shouldBe true
    player.is(player1.id) shouldBe true
    player.is(player2.id) shouldBe false
    player.is(player3.id) shouldBe true
  }

  "Player wins a point" in {
    player.win.points shouldBe 100
  }
