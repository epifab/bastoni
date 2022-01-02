package reactkonva

import konva.Konva
import konva.Konva.Vector2d
import org.scalajs.dom.*

import scala.scalajs.js

object KGroup extends ExternalComponentPropsAndChildren[KGroup.Props](ReactKonvaDOM.Group):
  @js.native
  trait Props extends ContainerProps
