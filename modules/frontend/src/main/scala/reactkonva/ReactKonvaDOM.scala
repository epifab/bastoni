package reactkonva

import konva.ShapeRef

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("react-konva", JSImport.Default)
object ReactKonvaDOM extends js.Object:
  val Stage: js.Object          = js.native
  val Layer: js.Object          = js.native
  val Rect: js.Object           = js.native
  val Line: js.Object           = js.native
  val Circle: js.Object         = js.native
  val Ellipse: js.Object        = js.native
  val Wedge: js.Object          = js.native
  val Image: js.Object          = js.native
  val Text: js.Object           = js.native
  val TextPath: js.Object       = js.native
  val Star: js.Object           = js.native
  val Ring: js.Object           = js.native
  val Arc: js.Object            = js.native
  val Label: js.Object          = js.native
  val Tag: js.Object            = js.native
  val Path: js.Object           = js.native
  val RegularPolygon: js.Object = js.native
  val Arrow: js.Object          = js.native
  val Shape: js.Object          = js.native
  val Sprite: js.Object         = js.native
  val Group: js.Object          = js.native
  val Node: js.Object           = js.native
  val Container: js.Object      = js.native

  @js.native
  trait Context extends js.Any:
    def beginPath(): Unit
    def moveTo(x: Double, y: Double): Unit
    def lineTo(x: Double, y: Double): Unit
    def quadraticCurveTo(x: Double, y: Double, w: Double, h: Double): Unit
    def closePath(): Unit
    def fillStrokeShape(shape: ShapeRef): Unit
    def rotate(x: Double): Unit
end ReactKonvaDOM
