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
          case PlayerState.ActingPlayer(_, _, timeout) =>
            TimeoutBar(
              center = layout.center,
              timeout = timeout,
              angle = Angle(220),
              rotation = layout.barsRotation,
              innerRadius = layout.radius,
              size = circleStrokeSize
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
