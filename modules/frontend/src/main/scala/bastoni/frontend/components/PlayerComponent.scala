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
        }
      )
    }

  def apply(state: PlayerState, layout: PlayerLayout): VdomElement = component(state -> layout)
