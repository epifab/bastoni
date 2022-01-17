package bastoni.frontend.model

import bastoni.domain.model.CardInstance

sealed trait Margin:
  def total(numberOfCards: Int): Double
  def perCard(numberOfCards: Int): Double

object Margin:
  case class Shared(size: Double) extends Margin:
    def perCard(numberOfCards:  Int): Double = size / numberOfCards
    def total(numberOfCards: Int): Double = if (numberOfCards == 0) 0 else size
  
  case class PerCard(size: Double) extends Margin:
    def perCard(numberOfCards: Int): Double = size
    def total(numberOfCards: Int): Double = size * (numberOfCards - 1)

object CardGroupRenderer:
  def apply(size: CardSize, center: Point, rotation: Angle, margin: Margin): CardsRenderer =
    (cards: List[CardInstance]) =>
      val blockWidth = size.width + margin.total(cards.length)
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
        margin = margin
      ))
