package bastoni.frontend.model

case class Size(width: Double, height: Double):
  def scale(factor: Double): Size     = Size(width * factor, height * factor)
  def scaleTo(newWidth: Double): Size = scale(newWidth / width)
