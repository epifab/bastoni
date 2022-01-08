package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.JsObject
import cats.effect
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.{Konva, TweenProps, TweenRef}
import konva.KonvaHelper.Vector2d
import org.scalajs.dom.html.Image
import org.scalajs.dom.{HTMLCanvasElement, document}
import reactkonva.*

import java.util.concurrent.atomic.AtomicReference
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
  case class Props(
    card: Card | Int,
    size: CardSize,
    position: (Double, Double),
    targetSize: Option[CardSize],
    targetPosition: Option[(Double, Double)]
  )

  private val cardImages: Map[Card, Image] =
    Deck.instance.map { card =>
      val suit = card.suit.toString.toLowerCase
      val rank = "%02d".format(card.rank.value)
      val img = Img(s"/static/carte/$suit/$rank.jpg")
      card -> img
    }.toMap

  private val cardBackImage: Image = Img("/static/carte/retro.jpg")

  private def renderCard(card: Option[Card]): HTMLCanvasElement =
    val image = new Konva.Image(
      JsObject[KImage.Props] { p =>
        p.width = FullCardSize.width
        p.height = FullCardSize.height
        p.image = card match {
          case Some(card) => cardImages(card)
          case None => cardBackImage
        }
      }
    )

    val group = new Konva.Group(
      JsObject[KGroup.Props] { p =>
        p.width = FullCardSize.width
        p.height = FullCardSize.height
        p.clipFunc = borderRadius(0, 0, FullCardSize.width, FullCardSize.height, FullCardSize.radius)
      }
    )

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

  class CardBackend($: BackendScope[Props, Unit]):
    private val groupRef = new AtomicReference[Option[TweenRef]](None)
    private val imageRef = new AtomicReference[Option[TweenRef]](None)

    val animation: Callback = for {
      props <- $.props
      _ <- props.targetSize.fold(Callback.empty)(targetSize => Callback(imageRef.get.foreach(r =>
        r.to(JsObject[TweenProps] { p =>
          p.width = targetSize.width
          p.height = targetSize.height
          p.duration = 0.5
        })
      )))
      _ <- props.targetPosition.fold(Callback.empty)(targetPosition => Callback(groupRef.get.foreach(r =>
        r.to(JsObject[TweenProps] { p =>
          p.x = targetPosition._1
          p.y = targetPosition._2
          p.duration = 0.5
        })
      )))
    } yield ()

    def render(props: Props): VdomNode =
      KGroup(
        { p =>
          p.ref = ref => groupRef.set(Some(ref))
          p.x = props.position._1
          p.y = props.position._2
        },
        KImage { p =>
          p.ref = ref => imageRef.set(Some(ref))
          p.image = renderCard(props.card match {
            case card: Card => Some(card)
            case _ => None
          })
          p.width = props.size.width
          p.height = props.size.height
          p.shadowColor = "#222"
          p.shadowBlur = props.size.radius
          p.shadowOpacity = 0.8
          p.shadowOffset = Vector2d(-props.size.radius, 0)
        },

        props.card match {
          case occurrences: Int if occurrences > 1 =>
            KGroup(
              KCircle(
                { p =>
                  p.radius = (props.size.width - 5) / 2
                  p.x = props.size.width / 2
                  p.y = props.size.height / 2
                  p.fill = "#222"
                  p.stroke = "#FFF"
                  p.strokeWidth = 3
                }
              ),
              KText(
                { p =>
                  p.text = occurrences.toString
                  p.height = props.size.height
                  p.width = props.size.width
                  p.fontFamily = "'Open Sans', sans-serif"
                  p.fontStyle = "bold"
                  p.fill = "#FFF"
                  p.align = "center"
                  p.verticalAlign = "middle"
                }
              )
            )
          case _ => KGroup.apply()
        }
      )

  private val component =
    ScalaComponent
      .builder[Props]
      .stateless
      .renderBackend[CardBackend]
      .componentDidMount(_.backend.animation)
      .build

  def apply(
    cardOrOccurrences: Card | Int,
    size: CardSize,
    position: (Double, Double),
    targetSize: Option[CardSize] = None,
    targetPosition: Option[(Double, Double)] = None
  ): VdomElement = component(Props(cardOrOccurrences, size, position, targetSize, targetPosition))
