package bastoni.frontend
package components

import bastoni.domain.model.*
import bastoni.frontend.JsObject
import bastoni.frontend.model.{CardLayout, CardSize, Palette, Shadow, Size}
import cats.effect
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.*
import konva.Konva.KonvaEventObject
import konva.KonvaHelper.Vector2d
import reactkonva.*

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

object CardComponent:

  case class Props(current: CardLayout, previous: Option[CardLayout], eventHandlers: Option[CardEventHandlers], selected: Boolean):
    val initial: CardLayout = previous.getOrElse(current)

  private class CardBackend($: BackendScope[Props, Unit]):
    private val animationDuration = .6

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
        KRect { rect =>
          addSelectable(props)(rect)
          addGlowingOrShadow(props)(rect)
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
          rect.fillPatternScale = Vector2d(
            Math.max(props.current.size.width / 400, .1),
            Math.max(props.current.size.width / 400, .1)
          )
          rect.stroke = Palette.grey2
          rect.strokeWidth = 2
          rect.cornerRadius = props.initial.size.cornerRadius
          rect.x = props.initial.size.cornerRadius * 2
          rect.y = props.initial.size.cornerRadius * 2
          rect.width = props.initial.size.width - (props.initial.size.cornerRadius * 4)
          rect.height = props.initial.size.height - (props.initial.size.cornerRadius * 4)
        }
      )

    private def renderCard(props: Props)(card: SimpleCard) =
      KImage { p =>
        addSelectable(props)(p)
        addGlowingOrShadow(props)(p)
        p.ref = cardSizeAnimation(props.current)
        p.image = Resources.cardImages(card)
        p.width = props.initial.size.width
        p.height = props.initial.size.height
      }

    private def addSelectable(props: Props)(p: NodeProps): Unit = {
      def setMousePointer(target: NodeRef, style: "default" | "pointer"): Callback =
        Callback(target.getStage().container().style.cursor = style)

      props.eventHandlers.foreach { handlers =>
        p.onMouseEnter = ref => (setMousePointer(ref.target, "pointer") *> handlers.onMouseOver).runNow()
        p.onMouseOut = ref => (setMousePointer(ref.target, "default") *> handlers.onMouseOut).runNow()
        p.onClick = ref => (setMousePointer(ref.target, "default") *> handlers.onSelect).runNow()
        p.onTap = ref => handlers.onSelect.runNow()
      }
    }

    private def addGlowingOrShadow(props: Props)(p: ShapeProps): Unit = {
      if (props.selected) {
        p.shadowBlur = 45
        p.shadowColor = Palette.cyan
        p.shadowOffset = Vector2d(0, 0)
        p.shadowOpacity = 1
      }
      else {
        props.current.shadow.foreach { shadow =>
          p.shadowBlur = shadow.size
          p.shadowColor = shadow.color
          p.shadowOffset = Vector2d(shadow.offset.x, shadow.offset.y)
          p.shadowOpacity = .5
        }
      }
    }

    private def animation[P <: TweenProps](f: P => Unit): TweenRef => Unit = tween =>
      Option(tween).foreach(_.to(JsObject[P](p => f(p))))

    def render(props: Props): VdomNode =
      KGroup(
        { p =>
          p.ref = animation[TweenProps] { p =>
            p.x = props.current.topLeft.x
            p.y = props.current.topLeft.y
            p.rotation = props.current.rotation.deg
            p.duration = animationDuration
          }
          p.x = props.initial.topLeft.x
          p.y = props.initial.topLeft.y
          p.rotation = props.initial.rotation.deg
        },
        props.current.card.toOption.map(_.simple).fold(renderCardBack(props))(renderCard(props))
      )

  private val component =
    ScalaComponent
      .builder[Props]
      .stateless
      .renderBackend[CardBackend]
      .shouldComponentUpdate(c => CallbackTo(
        c.currentProps.current != c.nextProps.current ||
          c.currentProps.eventHandlers.isDefined != c.nextProps.eventHandlers.isDefined ||
          c.currentProps.selected != c.nextProps.selected ||
          c.currentState != c.nextState
      ))
      .build

  def apply(
    layout: CardLayout,
    previous: Option[CardLayout],
    eventHandlers: Option[CardEventHandlers],
    selected: Boolean = false
  ): VdomElement =
    component
      .withKey(s"card-${layout.card.ref}")
      .apply(Props(layout, previous, eventHandlers, selected))


case class CardEventHandlers(
  onSelect: Callback,
  onMouseOver: Callback = Callback.empty,
  onMouseOut: Callback = Callback.empty
)
