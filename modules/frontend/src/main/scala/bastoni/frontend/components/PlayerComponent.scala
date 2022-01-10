package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.PlayerLayout
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import reactkonva.{KCircle, KGroup, KText}

object PlayerComponent:
  private val component =
    ScalaFnComponent[(PlayerState, PlayerLayout)] { case (state, layout) =>
      KGroup(
        KCircle { p =>
          p.radius = layout.radius
          p.x = layout.position.x
          p.y = layout.position.y
          p.fill = "#2B5B79"
        },
        KText { p =>
          p.text = state.name
          p.align = "center"
          p.verticalAlign = "middle"
          p.fill = "#FFF"
          p.fontFamily = "'Open Sans', sans-serif"
          p.fontStyle = "bold"
          p.fontSize = 16
          p.y = layout.position.y - layout.radius
          p.x = layout.position.x - layout.radius
          p.width = layout.radius * 2
          p.height = layout.radius * 2
        }
      )
    }

  def apply(state: PlayerState, layout: PlayerLayout): VdomElement = component(state -> layout)
