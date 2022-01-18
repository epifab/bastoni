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
import konva.*
import org.scalajs.dom.html.Image
import org.scalajs.dom.{HTMLCanvasElement, MouseEvent, document, window}
import reactkonva.*

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

object Image:
  def apply(src: String): Image =
    val img = document.createElement("img").asInstanceOf[Image]
    img.src = src
    img

object CardComponent:

  case class Props(current: CardLayout, previous: Option[CardLayout], selectable: Option[Callback]):
    val initial: CardLayout = previous.getOrElse(current)

  case class State(glowing: Boolean)

  val backOfCardImagePattern: Image = Image("/static/carte/cube.svg")

  val cardImages: Map[SimpleCard, Image] =
    Deck.cards.map { card =>
      val suit = card.suit.toString.toLowerCase
      val rank = "%02d".format(card.rank.value)
      val img = Image(s"/static/carte/napoletane/$suit/$rank.svg")
      card -> img
    }.toMap

  private class CardBackend($: BackendScope[Props, State]):
    private val animationDuration = .6

    private def withShadow(p: ShapeProps)(shadow: Shadow): Unit =
      p.shadowBlur = shadow.size
      p.shadowColor = Palette.grey3
      p.shadowOffset = Vector2d(shadow.offset.x, shadow.offset.y)
      p.shadowOpacity = .5

    def cardSizeAnimation(current: CardLayout): TweenRef => Unit = animation[TweenProps] { p =>
      p.width = current.size.width
      p.height = current.size.height
      p.duration = animationDuration
    }

    private def renderCardBack(props: Props): VdomNode =
      KGroup(
        { p =>
          p.ref = cardSizeAnimation(props.current)
          p.width = props.initial.size.width
          p.height = props.initial.size.height
        },
        KRect { p =>
          p.ref = animation[TweenProps & KRect.Props] { p =>
            p.width = props.current.size.width
            p.height = props.current.size.height
            p.cornerRadius = props.current.size.cornerRadius
            p.duration = animationDuration
          }
          p.width = props.initial.size.width
          p.height = props.initial.size.height
          p.cornerRadius = props.initial.size.cornerRadius
          p.fill = Palette.grey1
          props.current.shadow.foreach(withShadow(p))
        },
        KRect { p =>
          p.ref = animation[TweenProps & KRect.Props] { p =>
            p.cornerRadius = props.current.size.cornerRadius
            p.x = props.current.size.cornerRadius * 2
            p.y = props.current.size.cornerRadius * 2
            p.width = props.current.size.width - (props.current.size.cornerRadius * 4)
            p.height = props.current.size.height - (props.current.size.cornerRadius * 4)
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
          p.onMouseOver = ref => {
            $.setState(State(glowing = true)).runNow()
            setMousePointer(ref.target, "pointer")
          }
          p.onMouseOut = ref => {
            $.setState(State(glowing = false)).runNow()
            setMousePointer(ref.target, "default")
          }
          p.onClick = ref => {
            setMousePointer(ref.target, "default")
            $.setState(State(glowing = false), callback).runNow()
          }
          p.onTap = ref => callback.runNow()
        }
        p.ref = cardSizeAnimation(props.current)
        p.image = cardImages(card)
        p.width = props.initial.size.width
        p.height = props.initial.size.height
        if (state.glowing) {
          p.shadowBlur = 45
          p.shadowColor = "#1dc1d4"
          p.shadowOffset = Vector2d(0, 0)
          p.shadowOpacity = 1
        }
        else {
          props.current.shadow.foreach(withShadow(p))
        }
      }

    private def animation[P <: TweenProps](f: P => Unit): TweenRef => Unit = tween =>
      Option(tween).foreach(_.to(JsObject[P](p => f(p))))

    def render(props: Props, state: State): VdomNode =
      KGroup(
        { p =>
          p.ref = animation[TweenProps] { p =>
            p.x = props.current.position.x
            p.y = props.current.position.y
            p.rotation = props.current.rotation.deg
            p.duration = animationDuration
          }
          p.x = props.initial.position.x
          p.y = props.initial.position.y
          p.rotation = props.initial.rotation.deg
        },
        props.current.card.toOption.map(_.simple).fold(renderCardBack(props))(renderCard(props, state))
      )

  private val component =
    ScalaComponent
      .builder[Props]
      .initialState(State(glowing = false))
      .renderBackend[CardBackend]
      .shouldComponentUpdate(c => CallbackTo(
        c.currentProps.current != c.nextProps.current ||
          c.currentProps.selectable.isDefined != c.currentProps.selectable.isDefined ||
          c.currentState != c.nextState
      ))
      .build

  def apply(current: CardLayout, previous: Option[CardLayout], selectable: Option[Callback]): VdomElement =
    component
      .withKey(s"card-${current.card.ref}")
      .apply(Props(current, previous, selectable))
