package bastoni.frontend.model

case class Angle(deg: Int):
  val normalised: Int = deg % 360
  val rad: Double = deg * Math.PI / 180
  val sin: Double = Math.sin(rad)
  val cos: Double = Math.cos(rad)
  def +(offset: Int): Angle = Angle(deg + offset)
  def -(offset: Int): Angle = Angle(deg - offset)
  def *(scale: Double): Angle = Angle((deg * scale).floor.toInt)
  def unary_- : Angle = *(-1)

object Angle:
  val zero: Angle = Angle(0)
