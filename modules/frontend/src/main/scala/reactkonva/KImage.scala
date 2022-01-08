package reactkonva

import konva.Konva.{IRect, Vector2d}
import konva.{Konva, ShapeProps}
import org.scalajs.dom.*
import reactkonva.ReactKonvaDOM.Context

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.|

object KImage extends ExternalComponentWithoutChildren[KImage.Props](ReactKonvaDOM.Image):
  @js.native
  trait Props extends ShapeProps:
    var image: js.UndefOr[HTMLImageElement | HTMLCanvasElement] = js.native
    var crop: js.UndefOr[IRect] = js.native
