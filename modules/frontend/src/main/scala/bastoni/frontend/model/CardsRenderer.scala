package bastoni.frontend.model

import bastoni.domain.model.{CardInstance, HiddenCard, VisibleCard}

type CardsRenderer = List[CardInstance] => List[CardLayout]

type BoardRenderer = List[(Option[TablePlayer], CardInstance)] => List[CardLayout]

object CardsRenderer:
  def collapseFaceDownCards(cx: List[CardInstance], collapsed: List[HiddenCard]): List[VisibleCard | List[HiddenCard]] =
    cx match
      case (hidden: HiddenCard) :: tail => collapseFaceDownCards(tail, collapsed :+ hidden)
      case (instance: VisibleCard) :: tail if collapsed.isEmpty => instance :: collapseFaceDownCards(tail, Nil)
      case (instance: VisibleCard) :: tail => collapsed :: instance :: collapseFaceDownCards(tail, Nil)
      case Nil if collapsed.isEmpty => Nil
      case Nil => collapsed :: Nil
