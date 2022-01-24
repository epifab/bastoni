package bastoni.frontend.model

import bastoni.domain.model.CardInstance

sealed trait Margin:
  def total(itemSize: Double, numberOfItems: Int): Double
  def perCard(itemSize: Double, numberOfItems: Int): Double

object Margin:
  case object Default extends Margin:
    def total(itemSize: Double, numberOfItems: Int): Double = (itemSize + 2) * (numberOfItems - 1)
    def perCard(itemSize: Double, numberOfItems: Int): Double = itemSize + 2

  case class Total(size: Double) extends Margin:
    def total(itemSize: Double, numberOfItems: Int): Double = if (numberOfItems == 0) 0 else size
    def perCard(itemSize: Double, numberOfItems: Int): Double = size / numberOfItems
  
  case class PerCard(size: Double) extends Margin:
    def total(itemSize: Double, numberOfItems: Int): Double = size * (numberOfItems - 1)
    def perCard(itemSize: Double, numberOfItems: Int): Double = size
