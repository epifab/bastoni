package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.model.TableLayout
import bastoni.frontend.Resources
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.VdomElement
import konva.Konva
import konva.KonvaHelper.Vector2d
import org.scalajs.dom.html.Image
import reactkonva.{KLayer, KRect}

import scala.util.chaining.*

object TableLayer:
  private val component = ScalaFnComponent[TableLayout] { layout =>
    KLayer(
      KRect { p =>
        p.x = layout.topLeft.x
        p.y = layout.topLeft.y
        p.width = layout.size.width
        p.height = layout.size.height
        p.fillPatternX = layout.topLeft.x + (layout.size.width / 2)
        p.fillPatternY = layout.topLeft.y + (layout.size.height / 2)
        p.fillPatternImage = Resources.tablePatternImage
        p.fillPatternRepeat = "repeat"
        p.fillPriority = "pattern"
        p.fillPatternScale = Vector2d(1, 1)
      }
    )
  }

  def apply(layout: TableLayout): VdomElement = component(layout)
