package bastoni.frontend

import bastoni.domain.model.GameType
import cats.effect.unsafe.implicits.global
import cats.effect.IO
import org.scalajs.dom.{console, document}
import org.scalajs.dom.html.Image

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.duration.DurationInt

@main def run(): Unit =
  val root = document.getElementById("app-wrapper")

  Resources.onLoad(timeout = 5.seconds).unsafeRunAsync {
    case Right(_)    => components.MainComponent().renderIntoDOM(root)
    case Left(error) => console.error(error.getMessage)
  }
