package bastoni.frontend.model

case class Point(x: Double, y: Double):
  def *(scale: Double): Point = Point(x * scale, y * scale)
  def +(other: Point): Point = Point(x + other.x, y + other.y)
