package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.Palette
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.VdomElement
import reactkonva.{KArc, KCircle, KGroup, KText}

object PlayerComponent:
  private val circleStrokeSize: Int = 15

  private val component =
    ScalaFnComponent[(PlayerState, SeatLayout)] { case (state: PlayerState, layout: SeatLayout) =>
      val textRadius: Double = Math.sqrt(2 * layout.radius * layout.radius)
      val textTopLeftAngle: Angle = Angle(225 - layout.textRotation.deg)
      val textPosition: Point = Point(
        layout.center.x + textTopLeftAngle.sin * textRadius,
        layout.center.y + textTopLeftAngle.cos * textRadius
      )

      KGroup(
        KCircle { p =>
          p.radius = layout.radius
          p.x = layout.center.x
          p.y = layout.center.y
          p.fill = Palette.desaturatedBlue

          state match {
            case PlayerState.SittingOut(_) =>
              p.opacity = .4

            case PlayerState.EndOfMatchPlayer(_, true) =>
              p.stroke = Palette.yellow1
              p.strokeWidth = 10
              p.shadowColor = Palette.blue
              p.shadowBlur = 30
              p.shadowOpacity = 1

            case PlayerState.EndOfGamePlayer(_, _, true) =>
              p.stroke = Palette.yellow1
              p.strokeWidth = circleStrokeSize
              p.shadowColor = Palette.blue
              p.shadowBlur = circleStrokeSize
              p.shadowOpacity = 1

            case _ => ()
          }
        },
        state match {
          case PlayerState.ActingPlayer(_, _, Some(timeout)) =>
            KGroup(
              KArc { p =>
                p.x = layout.center.x
                p.y = layout.center.y
                p.angle = 360
                p.innerRadius = layout.radius
                p.outerRadius = layout.radius + circleStrokeSize
                p.fill = Palette.black
                p.shadowColor = Palette.blue
                p.shadowBlur = 30
                p.shadowOpacity = 1
              },
              KArc { p =>
                p.x = layout.center.x
                p.y = layout.center.y
                p.angle = (220 * timeout.value.toDouble / Timeout.Max.value).floor.toInt
                p.innerRadius = layout.radius
                p.outerRadius = layout.radius + circleStrokeSize
                p.rotation = layout.barsRotation.deg - 20
                p.fill = timeout match {
                  case Timeout.Max => Palette.green1
                  case Timeout.T9 => Palette.green2
                  case Timeout.T8 => Palette.green3
                  case Timeout.T7 => Palette.yellow1
                  case Timeout.T6 => Palette.yellow2
                  case Timeout.T5 => Palette.mustard
                  case Timeout.T4 => Palette.orange1
                  case Timeout.T3 => Palette.orange2
                  case Timeout.T2 => Palette.red1
                  case Timeout.T1 => Palette.red2
                  case Timeout.TimedOut => Palette.black
                }
              }
            )

          case PlayerState.ActingPlayer(_, _, None) =>
            KGroup(
              KArc { p =>
                p.x = layout.center.x
                p.y = layout.center.y
                p.angle = 360
                p.innerRadius = layout.radius
                p.outerRadius = layout.radius + circleStrokeSize
                p.fill = Palette.green1
                p.shadowColor = Palette.blue
                p.shadowBlur = circleStrokeSize
                p.shadowOpacity = 1
              }
            )

          case _ => KGroup()
        },
        KText { p =>
          p.text = state.name
          p.align = "center"
          p.verticalAlign = "middle"
          p.fill = Palette.white
          p.fontFamily = "'Open Sans', sans-serif"
          p.fontStyle = "bold"
          p.fontSize = 16
          p.x = textPosition.x
          p.y = textPosition.y
          p.width = layout.radius * 2
          p.height = layout.radius * 2
          p.rotation = layout.textRotation.deg
        }
      )
    }

  def apply(state: PlayerState, layout: SeatLayout): VdomElement = component(state -> layout)
