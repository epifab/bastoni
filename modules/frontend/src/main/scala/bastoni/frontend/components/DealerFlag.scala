package bastoni.frontend.components

import bastoni.domain.model.{CardId, HiddenCard}
import bastoni.frontend.model.{Angle, CardLayout, CardSize, Palette, Point, SeatLayout, Shadow}
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.vdom.VdomNode
import reactkonva.KGroup

object DealerFlag:
  private val component = ScalaFn[SeatLayout] { layout =>
    val tinyDeckSize             = CardSize.fixedWidth(20)
    val tinyDeckRelativeRotation = Angle(-45)
    val tinyDeckRotation         = layout.textRotation + 180 - tinyDeckRelativeRotation

    val offset1 = Point(
      tinyDeckRotation.cos * tinyDeckSize.height,
      tinyDeckRotation.sin * tinyDeckSize.height
    )

    val offset2 = Point(
      layout.textRotation.cos * (layout.radius - 10),
      layout.textRotation.sin * (layout.radius - 10)
    )

    CardComponent(
      CardLayout(
        card = HiddenCard(CardId.unknown),
        size = tinyDeckSize,
        topLeft = layout.center - offset1 + offset2,
        rotation = tinyDeckRotation,
        shadow = None
      ),
      previous = None,
      eventHandlers = None,
      selected = true
    )
  }

  def apply(layout: SeatLayout): VdomNode = component(layout)
end DealerFlag
