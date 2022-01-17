package bastoni.frontend.components

import bastoni.domain.model.*
import bastoni.frontend.model.TableLayout
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.VdomElement
import konva.Konva
import konva.KonvaHelper.Vector2d
import org.scalajs.dom.html.Image
import reactkonva.KRect

import scala.util.chaining.*

object TableComponent:
  val backgroundImage: Image = Image("/static/table.jpg")

  private val component = ScalaFnComponent[TableLayout] { layout =>
    KRect { p =>
      p.x = layout.topLeft.x
      p.y = layout.topLeft.y
      p.width = layout.size.width
      p.height = layout.size.height
      p.fillPatternX = layout.topLeft.x + (layout.size.width / 2)
      p.fillPatternY = layout.topLeft.y + (layout.size.height / 2)
      p.fillPatternImage = backgroundImage
      p.fillPatternRepeat = "repeat"
      p.fillPriority = "pattern"
      p.fillPatternScale = Vector2d(1, 1)
    }
  }

  def apply(layout: TableLayout): VdomElement = component(layout)
