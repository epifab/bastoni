package bastoni.frontend

import bastoni.domain.model.{Deck, SimpleCard}
import bastoni.frontend.model.CardSize
import cats.effect.IO
import org.scalajs.dom.*
import org.scalajs.dom.html.Image

import java.util.concurrent.atomic.AtomicInteger
import java.util.Base64
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits.*

object Image:
  private val domParser = new DOMParser()

  def apply(src: String, onload: Image => Unit): Image =
    val img = document.createElement("img").asInstanceOf[Image]
    img.src = src
    img.onload = _ => onload(img)
    img

  // Firefox is buggy and won't load svg images in some cases
  // this workaround comes from:
  // https://stackoverflow.com/questions/28690643/firefox-error-rendering-an-svg-image-to-html5-canvas-with-drawimage
  def svg(src: String, onLoad: Image => Unit): Image =
    val img = document.createElement("img").asInstanceOf[Image]

    fetch(src).flatMap(_.text()).foreach { text =>
      val result    = domParser.parseFromString(text, MIMEType.`text/xml`)
      val inlineSvg = result.getElementsByTagName("svg")(0)
      // Firefox needs width and height
      inlineSvg.setAttribute("width", s"${CardSize.full.width}px")
      inlineSvg.setAttribute("height", s"${CardSize.full.height}px")

      val svg64   = window.btoa(new XMLSerializer().serializeToString(inlineSvg))
      val image64 = s"data:image/svg+xml;base64,$svg64"

      img.src = image64
      img.onload = _ => onLoad(img)
    }

    img
end Image

trait Resources:
  def size: Int

  private val loadedCount: AtomicInteger = new AtomicInteger(0)

  protected def loaded(url: String): Unit =
    console.debug(s"$url loaded")
    loadedCount.incrementAndGet()

  def onLoad(timeout: FiniteDuration): IO[Unit] =
    val interval = 50.millis

    def check(attempts: Int): IO[Unit] =
      for
        loaded <- IO(loadedCount.get())
        _ <- {
          if (loaded == size)
            IO(
              console.debug(s"$loaded resources loaded successfully within ${(interval * attempts).toSeconds} seconds")
            )
          else if (attempts <= 0)
            IO.raiseError(
              RuntimeException(
                s"Timeout: only $loaded out of $size resources loaded within ${timeout.toSeconds} seconds"
              )
            )
          else IO.sleep(interval) *> check(attempts - 1)
        }
      yield ()

    check((timeout / interval).ceil.toInt)
end Resources

object Resources extends Resources:
  val cardStyle: "napoletane" | "piacentine" = "napoletane"

  val cardImages: Map[SimpleCard, Image] =
    Deck.cards.map { card =>
      val suit = card.suit.toString.toLowerCase
      val rank = "%02d".format(card.rank.value)
      val img = Image.svg(
        s"static/carte/napoletane/$suit/$rank.svg",
        img => loaded(s"static/carte/napoletane/$suit/$rank.svg")
      )
      card -> img
    }.toMap

//  val cardImages: Map[SimpleCard, HTMLCanvasElement] =
//    Deck.cards.map { card =>
//      val canvas = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
//      val suit = card.suit.toString.toLowerCase
//      val rank = "%02d".format(card.rank.value)
//      val url = s"static/carte/$cardStyle/$suit/$rank.svg"
//      canvas.width = CardSize.full.width.floor.toInt
//      canvas.height = CardSize.full.height.floor.toInt
//      Canvg
//        .from(canvas.getContext("2d"), url)
//        .`then`(_.render())
//        .`then`(_ => loaded(url))
//      card -> canvas
//    }.toMap

  val tablePatternImage: Image      = Image("static/table.jpg", img => loaded(img.src))
  val backOfCardPatternImage: Image = Image("static/carte/cube.svg", img => loaded(img.src))

  override val size: Int = cardImages.size + List(tablePatternImage, backOfCardPatternImage).length
end Resources
