package bastoni.frontend.model

import bastoni.domain.model.{CardInstance, HiddenCard, UserId, VisibleCard}

case class CardLayout(
  card: CardInstance,
  size: CardSize,
  position: Point,
  rotation: Angle,
  shadow: Option[Shadow]
)

case class CardGroupLayout(
  cards: List[CardInstance],
  cardSize: CardSize,
  topLeft: Point,
  rotation: Angle,
  shadow: Option[Shadow],
  margin: Margin
):
  def toCardLayout: List[CardLayout] = cards.zipWithIndex.map { case (card, index) =>
    CardLayout(
      card,
      cardSize,
      Point(
        x = topLeft.x + (rotation.cos * index * margin.perCard(cards.length)),
        y = topLeft.y + (rotation.sin * index * margin.perCard(cards.length))
      ),
      rotation,
      shadow
    )
  }

type CardsRenderer = List[CardInstance] => List[CardLayout | CardGroupLayout]

enum TablePlayer:
  case MainPlayer, Player1, Player2, Player3

type BoardRenderer = List[(Option[TablePlayer], CardInstance)] => List[CardLayout | CardGroupLayout]

object CardsRenderer:
  def collapseFaceDownCards(cx: List[CardInstance], collapsed: List[HiddenCard]): List[VisibleCard | List[HiddenCard]] =
    cx match
      case (hidden: HiddenCard) :: tail => collapseFaceDownCards(tail, collapsed :+ hidden)
      case (instance: VisibleCard) :: tail if collapsed.isEmpty => instance :: collapseFaceDownCards(tail, Nil)
      case (instance: VisibleCard) :: tail => collapsed :: instance :: collapseFaceDownCards(tail, Nil)
      case Nil if collapsed.isEmpty => Nil
      case Nil => collapsed :: Nil
