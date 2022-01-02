package reactkonva

import konva.Konva
import konva.Konva.Vector2d
import org.scalajs.dom.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

object KStage extends ExternalComponentPropsAndChildren[KStage.Props](ReactKonvaDOM.Stage):
  @js.native
  trait Props extends ContainerProps