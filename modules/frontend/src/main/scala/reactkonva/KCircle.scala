package reactkonva

import konva.{Konva, ShapeProps}
import konva.Konva.Vector2d
import org.scalajs.dom.*

import scala.scalajs.js

object KCircle extends ExternalComponentWithoutChildren[KCircle.Props](ReactKonvaDOM.Circle):
  @js.native
  trait Props extends ShapeProps:
    var radius: js.UndefOr[Double] = js.native
