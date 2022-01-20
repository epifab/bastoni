package bastoni.frontend.components

import bastoni.frontend.model.{DeckLayout, Palette}
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.vdom.VdomNode
import reactkonva.{KCircle, KGroup, KLayer, KText}

object DeckCountLayer:
  private val component = ScalaFn[(GameState, DeckLayout)] { case (gameState, layout) =>
    Some(gameState).filter(_.currentTable.deck.nonEmpty).fold(KLayer()) { game =>
      KLayer(
        KGroup(
          KCircle(
            { p =>
              p.radius = layout.radius
              p.x = layout.topLeft.x
              p.y = layout.topLeft.y
              p.fill = Palette.grey2
              p.stroke = Palette.white
              p.strokeWidth = 3
            }
          ),
          KText(
            { p =>
              p.x = layout.topLeft.x - layout.radius
              p.y = layout.topLeft.y - layout.radius
              p.text = game.currentTable.deck.size.toString
              p.height = layout.radius * 2
              p.width = layout.radius * 2
              p.fontFamily = "'Open Sans', sans-serif"
              p.fontSize = 20
              p.fontStyle = "bold"
              p.fill = Palette.white
              p.align = "center"
              p.verticalAlign = "middle"
            }
          )
        )
      )
    }
  }

  def apply(state: GameState, layout: DeckLayout): VdomNode = component(state -> layout)