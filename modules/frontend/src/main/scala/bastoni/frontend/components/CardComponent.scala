package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Generic.{Mounted, Unmounted}
import japgolly.scalajs.react.vdom.html_<^.*
import konva.Konva
import konva.KonvaHelper.Vector2d
import org.scalajs.dom.html.Image
import org.scalajs.dom.{HTMLCanvasElement, document}
import reactkonva.{KGroup, KImage, KLayer, KStage}

import scala.scalajs.js

case class CardProps(card: Card | Int, size: CardSize)

object Img:
  def apply(src: String): Image =
    val img = document.createElement("img").asInstanceOf[Image]
    img.src = src
    img

object CardComponent:
  private val cardImages: Map[Card, Image] =
    Deck.instance.map { card =>
      val suit = card.suit.toString.toLowerCase
      val rank = "%02d".format(card.rank.value)
      val img = Img(s"/static/carte/$suit/$rank.jpg")
      card -> img
    }.toMap

  private val cardBackImage: Image = Img("/static/carte/retro.jpg")

  private def renderCard(props: CardProps): HTMLCanvasElement =
    val imageProps = (new js.Object).asInstanceOf[KImage.Props]
    imageProps.width = props.size.width
    imageProps.height = props.size.height
    imageProps.image = props match {
      case CardProps(card: Card, size) => cardImages(card)
      case CardProps(occurrences: Int, size) => cardBackImage
    }

    val image = new Konva.Image(imageProps)

    val groupProps = (new js.Object).asInstanceOf[KGroup.Props]
    groupProps.width = props.size.width
    groupProps.height = props.size.height
    groupProps.clipFunc = borderRadius(0, 0, props.size.width, props.size.height, props.size.radius)

    val group = new Konva.Group(groupProps)

    group.add(image)
    group.toCanvas()

  private def borderRadius(x: Int, y: Int, width: Int, height: Int, radius: Int): reactkonva.ReactKonvaDOM.Context => Unit = ctx => {
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
    ScalaFnComponent[CardProps] { props =>
      KImage
        .builder
        .set(_.image = renderCard(props))
        .set(_.shadowColor = "#222")
        .set(_.shadowBlur = props.size.radius)
        .set(_.shadowOpacity = 0.8)
        .set(_.shadowOffset = Vector2d(-props.size.radius, 0))
        .build()
    }

  def apply(props: CardProps): VdomElement = component(props)


enum CardSize(val css: String, val width: Int, val height: Int, val radius: Int):
  case Small extends CardSize("sm", 28, 45, 2)
  case Medium extends CardSize("md", 45, 74, 5)
  case Large extends CardSize("lg", 90, 148, 10)

def CardsComponent(cards: List[CardPlayerView], size: CardSize): VdomNode =
  def compacted(cx: List[Option[Card]], count: Int): List[Card | Int] =
    cx match
      case None :: tail => compacted(tail, count + 1)
      case Some(card) :: tail if count == 0 => card :: compacted(tail, 0)
      case Some(card) :: tail => count :: card :: compacted(tail, 0)
      case Nil if count > 0 => count :: Nil
      case Nil => Nil

  KGroup.build(
    compacted(cards.reverse.map(_.card), 0)
      .zipWithIndex
      .map { case (c, index) =>
        KGroup
          .builder
          .set(_.x = Math.round(size.width * 0.8) * index)
          .build(CardComponent(CardProps(c, size)))
      }: _*
  )
