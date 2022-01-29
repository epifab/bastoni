package bastoni.frontend.components

import bastoni.domain.model.{CardId, Deck, VisibleCard}
import bastoni.frontend.model.*
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom.window
import reactkonva.{KGroup, KLayer, KRect, KStage}

object CardLayoutExample:
  def apply(): VdomNode = {
    val cards: List[VisibleCard] = Deck.cards.take(6).zipWithIndex.map { case (c, i) => VisibleCard(c.rank, c.suit, CardId(i)) }
    val cardSize = CardSize.fixedWidth(20)

    val layouts: List[Point => List[CardLayout]] = for {
      vAlign <- List(Align.Vertical.Top, Align.Vertical.Middle, Align.Vertical.Bottom)
      hAlign <- List(Align.Horizontal.Left, Align.Horizontal.Center, Align.Horizontal.Right)
      rotation <- List(Angle(0), Angle(30), Angle(60), Angle(90))
      layouts = (point: Point) => CardLayout.group(
        cards,
        cardSize,
        point,
        cardsPerRow = Some(4),
        rotation = rotation,
        vAlign = vAlign,
        hAlign = hAlign
      )
    } yield layouts

    val points = for {
      x <- List(100, 300, 500, 700, 900, 1100, 1300, 1500, 1700)
      y <- List(100, 300, 500, 700)
    } yield Point(x, y)

    val groups = layouts.zip(points).map { case (layout, point) =>
      KGroup(
        KGroup(layout(point).map(CardComponent(_, None, None)): _*),
        KRect({ p =>
          p.x = point.x - 2
          p.y = point.y - 2
          p.width = 4
          p.height = 4
          p.stroke = Palette.blue
          p.fill = Palette.blue
          p.strokeWidth = 2
        })
      )
    }

    KStage(
      { p =>
        p.width = window.innerWidth
        p.height = window.innerHeight
      },
      KLayer(groups: _*)
    )
  }