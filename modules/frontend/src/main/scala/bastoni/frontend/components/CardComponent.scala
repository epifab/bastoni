package bastoni.frontend
package components

import bastoni.domain.model.*
import bastoni.frontend.JsObject
import bastoni.frontend.model.{CardLayout, CardSize, Shadow, Size}
import cats.effect
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.Konva.KonvaEventObject
import konva.KonvaHelper.Vector2d
import konva.{Konva, NodeRef, ShapeProps, TweenProps, TweenRef}
import org.scalajs.dom.html.Image
import org.scalajs.dom.{HTMLCanvasElement, MouseEvent, document, window}
import reactkonva.*

import java.util.concurrent.atomic.AtomicReference
import scala.collection.MapView
import scala.scalajs.js

object Image:
  def apply(src: String): Image =
    val img = document.createElement("img").asInstanceOf[Image]
    img.src = src
    img

object CardComponent:

  case class Props(current: CardLayout, previous: Option[CardLayout], selectable: Option[Callback]):
    def initial: CardLayout = previous.getOrElse(current)

  case class State(glowing: Double)

  val backOfCardImagePattern: Image = Image("/static/carte/cube.svg")

  val cardImages: Map[SimpleCard, Image] =
    Deck.cards.map { card =>
      val suit = card.suit.toString.toLowerCase
      val rank = "%02d".format(card.rank.value)
      val img = Image(s"/static/carte/napoletane/$suit/$rank.svg")
      card -> img
    }.toMap

  class CardBackend($: BackendScope[Props, State]):
    private val animations = new AtomicReference[List[CardLayout => Unit]](Nil)
    private val animationDuration = .6

    private def withShadow(p: ShapeProps)(shadow: Shadow): Unit =
      p.shadowBlur = shadow.size
      p.shadowColor = Palette.grey3
      p.shadowOffset = Vector2d(shadow.offset.x, shadow.offset.y)
      p.shadowOpacity = .5

    def cardAnimationRef: TweenRef => Unit = animationRef[TweenProps] { (p, current) =>
      p.width = current.size.width
      p.height = current.size.height
      p.duration = animationDuration
    }

    private val fadeGlow: Callback =
      for {
        state <- $.state
        _ <- {
          if (state.glowing <= 0) Callback.empty
          else $.setState(State(state.glowing - .1), Callback(window.setTimeout(() => fadeGlow.runNow(), 100)))
        }
      } yield ()

    private def renderCardBack(props: Props): VdomNode =
      KGroup(
        { p =>
          p.ref = cardAnimationRef
          p.width = props.initial.size.width
          p.height = props.initial.size.height
        },
        KRect { p =>
          p.ref = animationRef[TweenProps & KRect.Props] { (p, current) =>
            p.width = current.size.width
            p.height = current.size.height
            p.cornerRadius = current.size.cornerRadius
            p.duration = animationDuration
          }
          p.width = props.initial.size.width
          p.height = props.initial.size.height
          p.cornerRadius = props.initial.size.cornerRadius
          p.fill = Palette.grey1
          props.current.shadow.foreach(withShadow(p))
        },
        KRect { p =>
          p.ref = animationRef[TweenProps & KRect.Props] { (p, current) =>
            // fillPatternScale doesn't work here
            // p.fillPatternScale = Vector2d(current.size.cornerRadius / 5, current.size.cornerRadius / 5)
            p.cornerRadius = current.size.cornerRadius
            p.x = current.size.cornerRadius * 2
            p.y = current.size.cornerRadius * 2
            p.width = current.size.width - (current.size.cornerRadius * 4)
            p.height = current.size.height - (current.size.cornerRadius * 4)
            p.duration = animationDuration
          }
          p.fillPatternImage = backOfCardImagePattern
          p.fillPatternRepeat = "repeat"
          p.fillPatternRotation = 270
          p.fillPatternScale = Vector2d(props.current.size.width / 400, props.current.size.width / 400)
          p.stroke = Palette.grey2
          p.strokeWidth = 2
          p.cornerRadius = props.initial.size.cornerRadius
          p.x = props.initial.size.cornerRadius * 2
          p.y = props.initial.size.cornerRadius * 2
          p.width = props.initial.size.width - (props.initial.size.cornerRadius * 4)
          p.height = props.initial.size.height - (props.initial.size.cornerRadius * 4)
        }
      )

    private def renderCard(props: Props, state: State)(card: SimpleCard) =
      def setMousePointer(target: NodeRef, style: "default" | "pointer"): Unit = {
        target.getStage().container().style.cursor = style
      }

      KImage { p =>
        props.selectable.foreach { callback =>
          p.onMouseOver = ref => setMousePointer(ref.target, "pointer")
          p.onMouseOut = ref => setMousePointer(ref.target, "default")
          p.onDblClick = ref => {
            setMousePointer(ref.target, "default")
            callback.runNow()
          }
          p.onDblTap = ref => {
            setMousePointer(ref.target, "default")
            callback.runNow()
          }
          p.onTap = _ => $.setState(State(glowing = 1), fadeGlow).runNow()
          p.onClick = _ => $.setState(State(glowing = 1), fadeGlow).runNow()
        }
        p.ref = cardAnimationRef
        p.image = cardImages(card)
        p.width = props.initial.size.width
        p.height = props.initial.size.height
        if (state.glowing > 0) {
          p.shadowBlur = 45 * state.glowing
          p.shadowColor = "#1dc1d4"
          p.shadowOffset = Vector2d(0, 0)
          p.shadowOpacity = 1
        }
        else {
          props.current.shadow.foreach(withShadow(p))
        }
      }

    private def animationRef[P <: TweenProps](f: (P, CardLayout) => Unit): TweenRef => Unit = tween =>
      val previousAnimations: List[CardLayout => Unit] = animations.get()
      animations.set(((current: CardLayout) => tween.to(JsObject[P](p => f(p, current)))) :: previousAnimations)

    val animate: Callback = for {
      props <- $.props
      _ <- if (props.previous.isEmpty) Callback.empty else Callback(animations.get.foreach(_(props.current)))
    } yield ()

    def render(props: Props, state: State): VdomNode =
      animations.set(Nil)

      KGroup(
        { p =>
          p.ref = animationRef[TweenProps] { (p, current) =>
            p.x = current.position.x
            p.y = current.position.y
            p.rotation = current.rotation.deg
            p.duration = animationDuration
          }
          p.x = props.initial.position.x
          p.y = props.initial.position.y
          p.rotation = props.initial.rotation.deg
        },
        props.current.card.toOption.map(_.simple).fold(renderCardBack(props))(renderCard(props, state))
      )

  private def component =
    ScalaComponent
      .builder[Props]
      .initialState(State(0))
      .renderBackend[CardBackend]
      .componentDidMount(_.backend.animate)
      .build

  def apply(current: CardLayout, previous: Option[CardLayout], selectable: Option[Callback]): VdomElement =
    component.withKey(s"card-${current.card.ref}")(Props(current, previous, selectable))
