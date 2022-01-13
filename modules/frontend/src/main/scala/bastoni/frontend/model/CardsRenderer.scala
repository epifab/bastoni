package bastoni.frontend.model

import bastoni.domain.model.{VisibleCard, CardInstance, HiddenCard}

case class CardLayout(
  card: CardInstance,
  size: CardSize,
  position: Point,
  rotation: Int,
  shadow: Option[Shadow]
)

case class CardGroupLayout(
  cards: List[CardInstance],
  cardSize: CardSize,
  topLeft: Point,
  rotation: Int,
  shadow: Option[Shadow],
  margin: Double
):
  def toCardLayout: List[CardLayout] = cards.zipWithIndex.map { case (card, index) =>
    CardLayout(
      card,
      cardSize,
      topLeft.copy(x = topLeft.x + (index * margin)),
      rotation,
      shadow
    )
  }

type CardsRenderer = List[CardInstance] => List[CardLayout | CardGroupLayout]

object CardsRenderer:
  def collapseFaceDownCards(cx: List[CardInstance], collapsed: List[HiddenCard]): List[VisibleCard | List[HiddenCard]] =
    cx match
      case (hidden: HiddenCard) :: tail => collapseFaceDownCards(tail, collapsed :+ hidden)
      case (instance: VisibleCard) :: tail if collapsed.isEmpty => instance :: collapseFaceDownCards(tail, Nil)
      case (instance: VisibleCard) :: tail => collapsed :: instance :: collapseFaceDownCards(tail, Nil)
      case Nil if collapsed.isEmpty => Nil
      case Nil => collapsed :: Nil
