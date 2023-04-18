package bastoni.domain

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

abstract class AsyncIOFreeSpec extends AsyncFreeSpecLike with AsyncIOSpec with Matchers:
  // This overrides the serial EC defined in ScalaTest, which doesn't seem to work well on ScalaJS
  override given executionContext: ExecutionContext = ExecutionContext.global
