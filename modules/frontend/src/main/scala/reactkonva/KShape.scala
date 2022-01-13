package reactkonva

import konva.ShapeProps

import scala.scalajs.js

object KShape extends ExternalComponentWithoutChildren[KShape.Props](ReactKonvaDOM.Shape):
  @js.native
  trait Props extends ShapeProps
