package bastoni.domain.logic
package briscola

import bastoni.domain.model.*
import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class BriscolaGameSpec extends AnyFreeSpec with Matchers:
  "Best card" - {
    "simple case" in {
      BriscolaGame.bestCard(Denari, List(Card(Quattro, Spade), Card(Asso, Coppe), Card(Due, Spade))) shouldBe Card(
        Quattro,
        Spade
      )
    }

    "better (same suit)" in {
      BriscolaGame.bestCard(Denari, List(Card(Quattro, Spade), Card(Asso, Coppe), Card(Cinque, Spade))) shouldBe Card(
        Cinque,
        Spade
      )
    }

    "better (briscola)" in {
      BriscolaGame.bestCard(Denari, List(Card(Quattro, Spade), Card(Asso, Coppe), Card(Due, Denari))) shouldBe Card(
        Due,
        Denari
      )
      BriscolaGame.bestCard(Denari, List(Card(Quattro, Spade), Card(Asso, Spade), Card(Due, Denari))) shouldBe Card(
        Due,
        Denari
      )
      BriscolaGame.bestCard(Denari, List(Card(Quattro, Spade), Card(Due, Denari), Card(Asso, Spade))) shouldBe Card(
        Due,
        Denari
      )
    }

    "briscola vs briscola" in {
      BriscolaGame.bestCard(Denari, List(Card(Asso, Denari), Card(Quattro, Denari))) shouldBe Card(Asso, Denari)
      BriscolaGame.bestCard(Denari, List(Card(Quattro, Denari), Card(Asso, Denari))) shouldBe Card(Asso, Denari)
    }
  }

  "Ordering" in {
    import BriscolaGame.>

    Card(Quattro, Denari) > Card(Cinque, Denari) shouldBe false
    Card(Cinque, Denari) > Card(Quattro, Denari) shouldBe true
    Card(Tre, Denari) > Card(Cinque, Denari) shouldBe true
    Card(Cinque, Denari) > Card(Tre, Denari) shouldBe false
  }
end BriscolaGameSpec
