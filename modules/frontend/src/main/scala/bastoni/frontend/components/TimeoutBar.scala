package bastoni.frontend.components

import bastoni.domain.model.Timeout
import bastoni.frontend.model.{Angle, Palette, Point}
import bastoni.frontend.Utils
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import japgolly.scalajs.react.ScalaComponent
import org.scalajs.dom.window
import reactkonva.{KArc, KGroup}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object TimeoutBar:
  val stepDuration: FiniteDuration = 3.seconds
  val steps                        = 100

  case class Props(
      center: Point,
      timeout: Option[Timeout],
      angle: Angle,
      rotation: Angle,
      innerRadius: Double,
      size: Double
  )

  case class State(factor: Double)

  class TimeoutBarBackend($ : BackendScope[Props, State]):
    val animate: Callback = for
      state <- $.state
      _ <- {
        if (state.factor == 0) Callback.empty
        else
          $.setState(
            State(Math.max(0, state.factor - (1.0 / steps))),
            Utils.timeoutCallback(animate, stepDuration / steps)
          )
      }
    yield ()

    def render(props: Props, state: State): VdomNode =
      props.timeout match
        case Some(timeout) =>
          KGroup(
            KArc { p =>
              p.x = props.center.x
              p.y = props.center.y
              p.angle = 360
              p.innerRadius = props.innerRadius
              p.outerRadius = props.innerRadius + props.size
              p.fill = Palette.black
              p.shadowColor = Palette.blue
              p.shadowBlur = props.size
              p.shadowOpacity = 1
            },
            KArc { p =>
              p.x = props.center.x
              p.y = props.center.y
              p.angle =
                ((props.angle.deg.toDouble / Timeout.Max.value) * (timeout.value - (1 - state.factor))).floor.toInt
              p.innerRadius = props.innerRadius
              p.outerRadius = props.innerRadius + props.size
              p.rotation = -props.rotation.deg - ((props.angle.deg - 180) / 2)
              p.fill = timeout match
                case Timeout.Max      => Palette.green1
                case Timeout.T9       => Palette.green2
                case Timeout.T8       => Palette.green3
                case Timeout.T7       => Palette.yellow1
                case Timeout.T6       => Palette.yellow2
                case Timeout.T5       => Palette.mustard
                case Timeout.T4       => Palette.orange1
                case Timeout.T3       => Palette.orange2
                case Timeout.T2       => Palette.red1
                case Timeout.T1       => Palette.red2
                case Timeout.TimedOut => Palette.black
            }
          )

        case None =>
          KArc { p =>
            p.x = props.center.x
            p.y = props.center.y
            p.angle = 360
            p.innerRadius = props.innerRadius
            p.outerRadius = props.innerRadius + props.size
            p.fill = Palette.green1
            p.shadowColor = Palette.blue
            p.shadowBlur = props.size
            p.shadowOpacity = 1
          }
  end TimeoutBarBackend

  private def component =
    ScalaComponent
      .builder[Props]
      .initialState(State(1))
      .renderBackend[TimeoutBarBackend]
      .componentDidMount(c => Utils.timeoutCallback(c.backend.animate, stepDuration / steps))
      .build

  def apply(
      center: Point,
      timeout: Option[Timeout],
      angle: Angle,
      rotation: Angle,
      innerRadius: Double,
      size: Double
  ): VdomNode = component(Props(center, timeout, angle, rotation, innerRadius, size))
end TimeoutBar
