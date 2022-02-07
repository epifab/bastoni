package bastoni.domain.logic

import bastoni.domain.AsyncIOFreeSpec
import bastoni.domain.model.GameType
import org.scalatest.Assertion
import scala.io.Source
import cats.effect.IO
import io.circe.parser.decode

class RegressionSpec extends AsyncIOFreeSpec:

  for {
    (gameType, gameLogic) <- List(
      (GameType.Briscola, briscola.BriscolaGame),
      (GameType.Tressette, tressette.TressetteGame),
      (GameType.Scopa, scopa.ScopaGame)
    )
    numberOfPlayers <- List(2, 3, 4)
  } yield {
    s"$gameType with $numberOfPlayers players" in {
      for {
        json <- IO(Source.fromResource(s"${gameType.toString.toLowerCase}-$numberOfPlayers-players-spec.json").getLines().mkString)
        content <- decode[RegressionSpecContent](json).fold(IO.raiseError(_), IO.pure)
        actualOutput = gameLogic.playStream(content.users)(fs2.Stream.iterable(content.input)).compile.toList
      } yield (actualOutput shouldBe content.output)
    }
  }
