package bastoni.frontend.model

import bastoni.domain.model.CardInstance

object CardGroupRenderer:
  def apply(size: CardSize, center: Point, rotation: Angle, margin: Double): CardsRenderer =
    (cards: List[CardInstance]) =>
      val blockWidth = size.width + (margin * (cards.size - 1))
      val blockHeight = size.height

      val topLeft = Point(
        center.x - rotation.cos * blockWidth / 2,
        center.y - rotation.sin * blockWidth / 2
      )

      List(CardGroupLayout(
        cards,
        size,
        topLeft,
        rotation,
        shadow = Some(Shadow(size.cornerRadius.floor.toInt, Point(-size.cornerRadius, 0).rotate(rotation))),
        marginX = margin
      ))
