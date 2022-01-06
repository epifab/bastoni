package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.VdomElement
import konva.Konva
import konva.KonvaHelper.Vector2d
import org.scalajs.dom.{HTMLCanvasElement, document}
import org.scalajs.dom.html.Image
import reactkonva.{KCircle, KGroup, KImage, KText}

import scala.scalajs.js

trait CardSize:
  def width: Double
  def height: Double
  def radius: Double

object FullCardSize extends CardSize:
  val width: Double = 90
  val height: Double = 148
  val radius: Double = 10

case class ScaledCardSize(original: CardSize, scale: Double) extends CardSize:
  val width: Double = original.width * scale
  val height: Double = original.height * scale
  val radius: Double = original.radius * scale

object ScaledCardSize:
  def width(w: Double): ScaledCardSize = ScaledCardSize(FullCardSize, w / FullCardSize.width)

object Img:
  def apply(src: String): Image =
    val img = document.createElement("img").asInstanceOf[Image]
    img.src = src
    img

object CardComponent:
  case class Props(card: Card | Int, size: CardSize)

  private val cardImages: Map[Card, Image] =
    Deck.instance.map { card =>
      val suit = card.suit.toString.toLowerCase
      val rank = "%02d".format(card.rank.value)
      val img = Img(s"/static/carte/$suit/$rank.jpg")
      card -> img
    }.toMap

  private val cardBackImage: Image = Img("/static/carte/retro.jpg")

  private def renderCard(props: Props): HTMLCanvasElement =
    val imageProps = (new js.Object).asInstanceOf[KImage.Props]
    imageProps.width = props.size.width
    imageProps.height = props.size.height
    imageProps.image = props.card match {
      case card: Card => cardImages(card)
      case _: Int => cardBackImage
    }

    val image = new Konva.Image(imageProps)

    val groupProps = (new js.Object).asInstanceOf[KGroup.Props]
    groupProps.width = props.size.width
    groupProps.height = props.size.height
    groupProps.clipFunc = borderRadius(0, 0, props.size.width, props.size.height, props.size.radius)

    val group = new Konva.Group(groupProps)

    group.add(image)
    group.toCanvas()

  private def borderRadius(x: Double, y: Double, width: Double, height: Double, radius: Double): reactkonva.ReactKonvaDOM.Context => Unit = ctx => {
    ctx.beginPath()
    ctx.moveTo(x + radius, y)
    ctx.lineTo(x + width - radius, y)
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
    ctx.lineTo(x + width, y + height - radius)
    ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
    ctx.lineTo(x + radius, y + height)
    ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
    ctx.lineTo(x, y + radius)
    ctx.quadraticCurveTo(x, y, x + radius, y)
    ctx.closePath()
  }

  private val component =
    ScalaFnComponent[Props] { props =>
      KGroup.build(
        KImage
          .builder
          .set(_.image = renderCard(props))
          .set(_.shadowColor = "#222")
          .set(_.shadowBlur = props.size.radius)
          .set(_.shadowOpacity = 0.8)
          .set(_.shadowOffset = Vector2d(-props.size.radius, 0))
          .build(),
        props.card match {
          case occurrences: Int if occurrences > 1 =>
            KGroup
              .build(
                KCircle
                  .builder
                  .set(_.radius = (props.size.width - 5) / 2)
                  .set(_.x = props.size.width / 2)
                  .set(_.y = props.size.height / 2)
                  .set(_.fill = "#222")
                  .set(_.stroke = "#FFF")
                  .set(_.strokeWidth = 3)
                  .build(),
                KText
                  .builder
                  .set(_.text = occurrences.toString)
                  .set(_.height = props.size.height)
                  .set(_.width = props.size.width)
                  .set(_.fontFamily = "'Open Sans', sans-serif")
                  .set(_.fontStyle = "bold")
                  .set(_.fill = "#FFF")
                  .set(_.align = "center")
                  .set(_.verticalAlign = "middle")
                  .build()
              )
          case _ => KGroup.build()
        }
      )
    }

  def apply(cardOrOccurrences: Card | Int, size: CardSize): VdomElement = component(Props(cardOrOccurrences, size))
