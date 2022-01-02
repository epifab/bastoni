package konva

import scala.scalajs.js

object KonvaHelper {
  object IRect {
    def apply(): Konva.IRect = apply(0, 0, 0, 0)

    def apply(x: Double, y: Double, width: Double, height: Double): Konva.IRect = js.Dynamic.literal(
      x = x,
      y = y,
      width = width,
      height = height,
    ).asInstanceOf[Konva.IRect]
  }

  object Vector2d {
    def apply(x: Int, y: Int): Konva.Vector2d = js.Dynamic.literal(
      x = x,
      y = y
    ).asInstanceOf[Konva.Vector2d]
  }
}
