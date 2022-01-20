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
import konva.*
import konva.Konva.KonvaEventObject
import konva.KonvaHelper.Vector2d
import reactkonva.*

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

object CardComponent:

  case class Props(current: CardLayout, previous: Option[CardLayout], selectable: Option[Callback]):
    val initial: CardLayout = previous.getOrElse(current)

  case class State(glowing: Boolean)

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

    private def renderCardBack(props: Props, state: State): VdomNode =
      KGroup(
        { p =>
          p.ref = cardSizeAnimation(props.current)
          p.width = props.initial.size.width
          p.height = props.initial.size.height
        },
        KRect { rect =>
          addSelectable(props)(rect)
          addGlowingOrShadow(props, state)(rect)
          rect.ref = animation[TweenProps & KRect.Props] { p =>
            p.width = props.current.size.width
            p.height = props.current.size.height
            p.cornerRadius = props.current.size.cornerRadius
            p.duration = animationDuration
          }
          rect.width = props.initial.size.width
          rect.height = props.initial.size.height
          rect.cornerRadius = props.initial.size.cornerRadius
          rect.fill = Palette.grey1
        },
        KRect { rect =>
          addSelectable(props)(rect)
          rect.ref = animation[TweenProps & KRect.Props] { p =>
            p.cornerRadius = props.current.size.cornerRadius
            p.x = props.current.size.cornerRadius * 2
            p.y = props.current.size.cornerRadius * 2
            p.width = props.current.size.width - (props.current.size.cornerRadius * 4)
            p.height = props.current.size.height - (props.current.size.cornerRadius * 4)
            p.duration = animationDuration
          }
          rect.fillPatternImage = Resources.backOfCardPatternImage
          rect.fillPatternRepeat = "repeat"
          rect.fillPatternRotation = 270
          rect.fillPatternScale = Vector2d(props.current.size.width / 400, props.current.size.width / 400)
          rect.stroke = Palette.grey2
          rect.strokeWidth = 2
          rect.cornerRadius = props.initial.size.cornerRadius
          rect.x = props.initial.size.cornerRadius * 2
          rect.y = props.initial.size.cornerRadius * 2
          rect.width = props.initial.size.width - (props.initial.size.cornerRadius * 4)
          rect.height = props.initial.size.height - (props.initial.size.cornerRadius * 4)
        }
      )

    private def renderCard(props: Props, state: State)(card: SimpleCard) =
      KImage { p =>
        addSelectable(props)(p)
        addGlowingOrShadow(props, state)(p)
        p.ref = cardSizeAnimation(props.current)
        p.image = Resources.cardImages(card)
        p.width = props.initial.size.width
        p.height = props.initial.size.height
      }

    private def addSelectable(props: Props)(p: NodeProps): Unit = {
      def setMousePointer(target: NodeRef, style: "default" | "pointer"): Unit =
        target.getStage().container().style.cursor = style

      props.selectable.foreach { callback =>
        p.onMouseEnter = ref => {
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
    }

    private def addGlowing(state: State)(p: ShapeProps): Unit =
      p.shadowBlur = 45
      p.shadowColor = "#1dc1d4"
      p.shadowOffset = Vector2d(0, 0)
      p.shadowOpacity = 1

    private def addGlowingOrShadow(props: Props, state: State)(p: ShapeProps): Unit =
      if (state.glowing) addGlowing(state)(p)
      else props.current.shadow.foreach(withShadow(p))

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
        props.current.card.toOption.map(_.simple).fold(renderCardBack(props, state))(renderCard(props, state))
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

  def apply(layout: CardLayout, previous: Option[CardLayout], selectable: Option[Callback]): VdomElement =
    component
      .withKey(s"card-${layout.card.ref}")
      .apply(Props(layout, previous, selectable))
