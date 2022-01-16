package bastoni.frontend

import bastoni.domain.model.GameType
import bastoni.frontend.components.{CardComponent, GameComponent, TableComponent}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalajs.dom.html.Image
import org.scalajs.dom.{console, document}

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.duration.DurationInt

@main def run(): Unit =
  val root = document.getElementById("app-wrapper")

  val loadedCount: AtomicInteger = new AtomicInteger(0)

  val images =
    CardComponent.backOfCardImagePattern ::
    CardComponent.cardImages.values.toList

  images.foreach(_.onload = _ => loadedCount.incrementAndGet())

  def onLoad(attempts: Int): IO[Unit] =
    for {
      _ <- if (attempts <= 0) IO.raiseError(RuntimeException("Images didn't load")) else IO.unit
      count <- IO(loadedCount.get())
      _ <- if (count == images.length) IO.unit else IO.sleep(50.millis) *> onLoad(attempts - 1)
    } yield ()

  onLoad(100).unsafeRunAsync {
    case Right(_) => components.GameComponent(GameType.Briscola).renderIntoDOM(root)
    case Left(error) => console.error("Troubles fetching images")
  }
