package bastoni.frontend.model

case class CardSize(size: Size, borderRadius: Double):
  def width: Double = size.width
  def height: Double = size.height
  def scale(factor: Double): CardSize = CardSize(size.scale(factor), borderRadius * factor)
  def scaleTo(newWidth: Double): CardSize = scale(newWidth / size.width)

object CardSize:
  def scaleTo(width: Double): CardSize = full.scaleTo(width)
  val full: CardSize = CardSize(Size(90, 148), borderRadius = 10)
