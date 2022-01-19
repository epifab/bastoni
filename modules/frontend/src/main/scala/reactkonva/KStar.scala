package reactkonva

import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Js.Component
import japgolly.scalajs.react.vdom.TagMod
import konva.{Konva, ShapeProps}
import org.scalajs.dom.*
import reactkonva.ReactKonvaDOM.Context

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

object KStar extends ExternalComponentWithoutChildren[KStar.Props](ReactKonvaDOM.Star):
  @js.native
  trait Props extends ShapeProps:
    var numPoints: js.UndefOr[Int] = js.native
    var innerRadius: js.UndefOr[Double] = js.native
    var outerRadius: js.UndefOr[Double] = js.native
