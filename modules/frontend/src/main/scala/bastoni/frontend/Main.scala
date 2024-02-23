package bastoni.frontend

import cats.effect.unsafe.implicits.global
import org.scalajs.dom.{console, document}

import scala.concurrent.duration.DurationInt

@main def run(): Unit =
  val root = document.getElementById("app-wrapper")

  Resources.onLoad(timeout = 5.seconds).unsafeRunAsync {
    case Right(_)    => components.MainComponent().renderIntoDOM(root)
    case Left(error) => console.error(error.getMessage)
  }
