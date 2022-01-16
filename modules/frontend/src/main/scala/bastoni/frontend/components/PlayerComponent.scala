package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import reactkonva.{KArc, KCircle, KGroup, KText}

object PlayerComponent:
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
          p.fill = "#2B5B79"

          state match {
            case PlayerState.SittingOut(_) =>
              p.opacity = .4

            case PlayerState.EndOfMatchPlayer(_, true) =>
              p.stroke = "#FFEB3B"
              p.strokeWidth = 10
              p.shadowColor = "#1f34ba"
              p.shadowBlur = 30
              p.shadowOpacity = 1

            case PlayerState.EndOfGamePlayer(_, _, true) =>
              p.stroke = "#FFEB3B"
              p.strokeWidth = 15
              p.shadowColor = "#1f34ba"
              p.shadowBlur = 30
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
                p.outerRadius = layout.radius + 30
                p.fill = "#000"
                p.shadowColor = "#1f34ba"
                p.shadowBlur = 30
                p.shadowOpacity = 1
              },
              KArc { p =>
                p.x = layout.center.x
                p.y = layout.center.y
                p.angle = (220 * timeout.value.toDouble / Timeout.Max.value).floor.toInt
                p.innerRadius = layout.radius
                p.outerRadius = layout.radius + 30
                p.rotation = layout.barsRotation.deg - 20
                p.fill = timeout match {
                  case Timeout.Max => "#00a650"
                  case Timeout.T9 => "#3ab54b"
                  case Timeout.T8 => "#8ec63f"
                  case Timeout.T7 => "#cadb2a"
                  case Timeout.T6 => "#fef200"
                  case Timeout.T5 => "#ffc20d"
                  case Timeout.T4 => "#f8931d"
                  case Timeout.T3 => "#f36523"
                  case Timeout.T2 => "#ed1b24"
                  case Timeout.T1 => "#ba131a"
                  case Timeout.TimedOut => "#000"
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
                p.outerRadius = layout.radius + 30
                p.fill = "#00a650"
                p.shadowColor = "#1f34ba"
                p.shadowBlur = 30
                p.shadowOpacity = 1
              }
            )

          case _ => KGroup()
        },
        KText { p =>
          p.text = state.name
          p.align = "center"
          p.verticalAlign = "middle"
          p.fill = "#FFF"
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
