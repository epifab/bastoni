package bastoni.backend

import bastoni.domain.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class GamePlayerSpec extends AnyFreeSpec with Matchers:
  val player1 = Player(PlayerId.newId, "John")
  val player2 = Player(PlayerId.newId, "John")
  val player3 = Player(player1.id, "Jack")

  val gamePlayer = GamePlayer(player1, 99)

  "Comparing 2 players" in {
    gamePlayer.is(player1) shouldBe true
    gamePlayer.is(player2) shouldBe false
    gamePlayer.is(player3) shouldBe true
  }

  "Player wins a point" in {
    gamePlayer.win.points shouldBe 100
  }
