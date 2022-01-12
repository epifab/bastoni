package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.model.TableLayout
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import reactkonva.KRect

import scala.util.chaining.*

object TableComponent:
  private val component = ScalaFnComponent[TableLayout] { layout =>
      KRect { p =>
        p.cornerRadius = 30
        p.x = layout.topLeft.x
        p.y = layout.topLeft.y
        p.width = layout.size.width
        p.height = layout.size.height
        p.fill = "rgba(8,166,53.1)"
      }
    }

  def apply(layout: TableLayout): VdomElement = component(layout)
