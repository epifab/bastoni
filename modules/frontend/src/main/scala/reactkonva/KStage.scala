package reactkonva

import konva.{Konva, ContainerProps}
import konva.Konva.Vector2d
import org.scalajs.dom.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

object KStage extends ExternalComponentWithChildren[KStage.Props](ReactKonvaDOM.Stage):
  @js.native
  trait Props extends ContainerProps