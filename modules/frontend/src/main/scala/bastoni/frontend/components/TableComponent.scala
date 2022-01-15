package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.model.TableLayout
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.VdomElement
import konva.Konva
import konva.KonvaHelper.Vector2d
import reactkonva.KRect

import scala.util.chaining.*

object TableComponent:
  private val component = ScalaFnComponent[TableLayout] { layout =>
      KRect { p =>
        p.x = layout.topLeft.x
        p.y = layout.topLeft.y
        p.width = layout.size.width
        p.height = layout.size.height
        p.fill = "rgba(8,166,53.1)"
//        todo: not working
//        p.fillRadialGradientStartPoint = Vector2d(0, 0)
//        p.fillRadialGradientEndPoint = Vector2d(0, 0)
//        p.fillRadialGradientStartRadius = 0
//        p.fillRadialGradientEndRadius = 70
//        p.fillRadialGradientColorStops = List(.9, "#136c27", .65, "#11461d", 1, "#08a635")
      }
    }

  def apply(layout: TableLayout): VdomElement = component(layout)
