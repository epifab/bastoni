package bastoni.frontend.model

case class CardSize(size: Size, cornerRadius: Double):
  def width: Double                       = size.width
  def height: Double                      = size.height
  def scale(factor: Double): CardSize     = CardSize(size.scale(factor), cornerRadius * factor)
  def *(factor: Double): CardSize         = scale(factor)
  def /(factor: Double): CardSize         = scale(1 / factor)
  def scaleTo(newWidth: Double): CardSize = scale(newWidth / size.width)

object CardSize:
  def fixedWidth(width: Double): CardSize                    = full.scaleTo(width)
  def scaleTo(maxWidth: Double, maxHeight: Double): CardSize = full.scaleTo(Math.min(maxWidth, maxHeight * ratioW))
  val full: CardSize                                         = CardSize(Size(87, 140), cornerRadius = 5)
  val ratioW: Double                                         = full.width / full.height
