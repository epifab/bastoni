package bastoni.domain

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import org.scalatest.freespec.AsyncFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext

abstract class AsyncIOFreeSpec extends AsyncFreeSpecLike with AsyncIOSpec with Matchers:
  // This overrides the serial EC defined in ScalaTest, which doesn't seem to work well on ScalaJS
  override given executionContext: ExecutionContext = ExecutionContext.global

  given Logger[IO] = Slf4jLogger.getLogger
