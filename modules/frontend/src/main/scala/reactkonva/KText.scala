package reactkonva

import konva.{Konva, ShapeProps}
import konva.Konva.Vector2d
import org.scalajs.dom.*

import scala.scalajs.js

object KText extends ExternalComponentWithoutChildren[KText.Props](ReactKonvaDOM.Text):
  @js.native
  trait Props extends ShapeProps:
    var text: js.UndefOr[String]          = js.native
    var fontFamily: js.UndefOr[String]    = js.native
    var fontSize: js.UndefOr[Int]         = js.native
    var fontStyle: js.UndefOr[String]     = js.native
    var align: js.UndefOr[String]         = js.native
    var verticalAlign: js.UndefOr[String] = js.native
    var padding: js.UndefOr[Int]          = js.native
    var lineHeight: js.UndefOr[Double]    = js.native
    var wrap: js.UndefOr[String]          = js.native
    var ellipsis: js.UndefOr[Boolean]     = js.native
