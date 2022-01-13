package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.model.SeatLayout
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import reactkonva.{KCircle, KGroup, KText}

object PlayerComponent:
  private val component =
    ScalaFnComponent[(PlayerState, SeatLayout)] { case (state, layout) =>
      KGroup(
        KCircle { p =>
          p.radius = layout.radius
          p.x = layout.center.x
          p.y = layout.center.y
          p.fill = "#2B5B79"

          state match {
            case PlayerState.ActingPlayer(_, _, timeout) =>
              p.stroke = "#0EF"
              p.strokeWidth = 10
              p.shadowColor = "#1f34ba"
              p.shadowBlur = 30
              p.shadowOpacity = 1

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
        KText { p =>
          p.text = state.name
          p.align = "center"
          p.verticalAlign = "middle"
          p.fill = "#FFF"
          p.fontFamily = "'Open Sans', sans-serif"
          p.fontStyle = "bold"
          p.fontSize = 16
          p.y = layout.center.y - layout.radius
          p.x = layout.center.x - layout.radius
          p.width = layout.radius * 2
          p.height = layout.radius * 2
        }
      )
    }

  def apply(state: PlayerState, layout: SeatLayout): VdomElement = component(state -> layout)
