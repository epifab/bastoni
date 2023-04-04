package bastoni.frontend.model

case class Point(x: Double, y: Double):
  def *(scale: Double): Point = Point(x * scale, y * scale)
  def +(other: Point): Point  = Point(x + other.x, y + other.y)
  def -(other: Point): Point  = Point(x - other.x, y - other.y)
  def unary_- : Point         = Point(-x, -y)

  def rotate(angle: Angle): Point =
    Point(
      angle.cos * x - angle.sin * y,
      angle.cos * y + angle.sin * x
    )
