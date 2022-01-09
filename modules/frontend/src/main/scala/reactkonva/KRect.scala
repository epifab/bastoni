package reactkonva

import konva.Konva.Vector2d
import konva.{Konva, ShapeProps}
import org.scalajs.dom.*

import scala.scalajs.js

object KRect extends ExternalComponentWithoutChildren[KRect.Props](ReactKonvaDOM.Rect):
  @js.native
  trait Props extends ShapeProps:
    var cornerRadius: js.UndefOr[Double] = js.native
