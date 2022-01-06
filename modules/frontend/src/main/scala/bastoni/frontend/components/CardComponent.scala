package bastoni.frontend.components

import bastoni.domain.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Generic.{Mounted, Unmounted}
import japgolly.scalajs.react.vdom.html_<^.*
import konva.Konva
import konva.KonvaHelper.Vector2d
import org.scalajs.dom.html.Image
import org.scalajs.dom.{HTMLCanvasElement, document, window}
import reactkonva.{KGroup, KImage, KLayer, KStage}

import scala.scalajs.js

case class CardProps(card: Card | Int, width: Double, height: Double, radius: Double)

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
    imageProps.width = props.width
    imageProps.height = props.height
    imageProps.image = props.card match {
      case card: Card => cardImages(card)
      case _: Int => cardBackImage
    }

    val image = new Konva.Image(imageProps)

    val groupProps = (new js.Object).asInstanceOf[KGroup.Props]
    groupProps.width = props.width
    groupProps.height = props.height
    groupProps.clipFunc = borderRadius(0, 0, props.width, props.height, props.radius)

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
    ScalaFnComponent[CardProps] { props =>
      KImage
        .builder
        .set(_.image = renderCard(props))
        .set(_.shadowColor = "#222")
        .set(_.shadowBlur = props.radius)
        .set(_.shadowOpacity = 0.8)
        .set(_.shadowOffset = Vector2d(-props.radius, 0))
        .build()
    }

  def apply(props: CardProps): VdomElement = component(props)


enum CardSize(val width: Int, val height: Int, val radius: Int):
  case Small extends CardSize(28, 45, 2)
  case Medium extends CardSize(45, 74, 5)
  case Full extends CardSize(90, 148, 10)

def CardsComponent(cards: List[CardPlayerView]): VdomNode =
  val containerWidth: Double = window.innerWidth
  val containerHeight: Double = window.innerHeight

  val cardOffsetFactorX = 0.8
  val cardOffsetFactorY = 0.7

  val cardWidth: Double = Math.min(CardSize.Full.width, (containerWidth / ((4 * cardOffsetFactorX) + 1)).floor)
  val cardHeight: Double = (cardWidth / CardSize.Full.width) * CardSize.Full.height
  val cardRadius: Double = (cardWidth / CardSize.Full.width) * CardSize.Full.radius

  val cardOffsetX: Double = cardWidth * cardOffsetFactorX
  val cardOffsetY: Double = cardHeight * cardOffsetFactorY

  def faceDownCompacted(cx: List[Option[Card]], count: Int): List[Card | Int] =
    cx match
      case None :: tail => faceDownCompacted(tail, count + 1)
      case Some(card) :: tail if count == 0 => card :: faceDownCompacted(tail, 0)
      case Some(card) :: tail => count :: card :: faceDownCompacted(tail, 0)
      case Nil if count > 0 => count :: Nil
      case Nil => Nil

  val cardsPerRow: Int = ((containerWidth - cardWidth) / cardOffsetX).floor.toInt + 1
  val numberOfRows: Int = (cards.size / cardsPerRow.toDouble).ceil.toInt
  val verticalOffset: Double = containerHeight - cardHeight - ((numberOfRows - 1) * cardOffsetY)

  KGroup
    .builder
    .build(
      faceDownCompacted(cards.map(_.card), 0)
        .grouped(cardsPerRow)
        .zipWithIndex
        .flatMap { case (cards, row) =>
          val rowSize: Double = cardWidth + (cardOffsetX * (cards.size - 1))
          val horizontalOffset: Double = Math.max(0, containerWidth - rowSize) / 2.0
          cards.zipWithIndex
            .map { case (card, col) =>
              KGroup
                .builder
                .set(_.width = containerWidth)
                .set(_.height = containerHeight)
                .set(_.x = horizontalOffset + (cardOffsetX * col))
                .set(_.y = verticalOffset + (row * cardOffsetY))
                .build(CardComponent(CardProps(card, cardWidth, cardHeight, cardRadius)))
            }
        }.toSeq: _*
    )
