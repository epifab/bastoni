package bastoni.frontend.model

case class Point(x: Double, y: Double):
  def *(scale: Double): Point = Point(x * scale, y * scale)
  def +(other: Point): Point = Point(x + other.x, y + other.y)

  def rotate(angle: Angle): Point =
    val r = Math.sqrt(x * x + y * y)
    Point(-angle.cos * r, -angle.sin * r)
