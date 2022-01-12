package bastoni.frontend
package components

import bastoni.domain.model.*
import bastoni.frontend.JsObject
import bastoni.frontend.model.{CardLayout, CardSize}
import cats.effect
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.KonvaHelper.Vector2d
import konva.{Konva, TweenProps, TweenRef}
import org.scalajs.dom.html.Image
import org.scalajs.dom.{HTMLCanvasElement, document}
import reactkonva.*

import java.util.concurrent.atomic.AtomicReference
import scala.collection.MapView
import scala.scalajs.js

object Img:
  def apply(src: String): Image =
    val img = document.createElement("img").asInstanceOf[Image]
    img.src = src
    img

object CardComponent:

  case class Props(current: CardLayout, previous: Option[CardLayout]):
    def previousOrCurrent: CardLayout = previous.getOrElse(current)

  private val cardImages: Map[Option[SimpleCard], Image] =
    Map(None -> Img("/static/carte/retro.jpg")) ++ Deck.cards.map { card =>
      val suit = card.suit.toString.toLowerCase
      val rank = "%02d".format(card.rank.value)
      val img = Img(s"/static/carte/$suit/$rank.jpg")
      Some(card) -> img
    }.toMap

  private val konvaCards: MapView[Option[SimpleCard], HTMLCanvasElement] = cardImages.view.mapValues { image =>
    val kimage = new Konva.Image(
      JsObject[KImage.Props] { p =>
        p.width = CardSize.full.width
        p.height = CardSize.full.height
        p.image = image
      }
    )

    val kgroup = new Konva.Group(
      JsObject[KGroup.Props] { p =>
        p.width = CardSize.full.width
        p.height = CardSize.full.height
        p.clipFunc = borderRadius(0, 0, CardSize.full.width, CardSize.full.height, CardSize.full.borderRadius)
      }
    )

    kgroup.add(kimage)
    kgroup.toCanvas()
  }

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
    private val animationDuration = .6

    val animation: Callback = for {
      props <- $.props
      _ <- if (props.previous.isEmpty) Callback.empty else Callback(imageRef.get.foreach(r =>
        r.to(JsObject[TweenProps] { p =>
          p.width = props.current.size.width
          p.height = props.current.size.height
          p.duration = animationDuration
        })
      ))
      _ <- if (props.previous.isEmpty) Callback.empty else Callback(groupRef.get.foreach(r =>
        r.to(JsObject[TweenProps] { p =>
          p.x = props.current.position.x
          p.y = props.current.position.y
          p.rotation = props.current.rotation
          p.duration = animationDuration
        })
      ))
    } yield ()

    def render(props: Props): VdomNode =
      KGroup(
        { p =>
          p.ref = ref => groupRef.set(Some(ref))
          p.x = props.previousOrCurrent.position.x
          p.y = props.previousOrCurrent.position.y
          p.rotation = props.previousOrCurrent.rotation
        },
        KImage { p =>
          p.ref = ref => imageRef.set(Some(ref))
          p.image = konvaCards(props.current.card.toOption.map(_.simple))
          p.width = props.previousOrCurrent.size.width
          p.height = props.previousOrCurrent.size.height
          p.shadowBlur = props.current.shadowSize
          p.shadowOffset = Vector2d(-props.current.shadowSize, 0)
          p.shadowColor = "#222"
          p.shadowOpacity = 0.8
        }
      )

  private def component =
    ScalaComponent
      .builder[Props]
      .stateless
      .renderBackend[CardBackend]
      .componentDidMount(_.backend.animation)
      .build

  def apply(current: CardLayout, previous: Option[CardLayout]): VdomElement =
    component.withKey(s"card-${current.card.ref}")(Props(current, previous))
