package reactkonva

import konva.{Konva, ContainerProps}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import konva.Konva.Vector2d
import org.scalajs.dom.*

object KLayer extends ExternalComponentWithChildren[KLayer.Props](ReactKonvaDOM.Layer):
  @js.native
  trait Props extends ContainerProps:
    // Layer
    // clearBeforeDraw: js.UndefOr[Boolean] = js.undefined, // this is redundant in Konva
    var hitGraphEnabled: js.UndefOr[Boolean] = js.native
    var imageSmoothingEnabled: js.UndefOr[Boolean] = js.native
