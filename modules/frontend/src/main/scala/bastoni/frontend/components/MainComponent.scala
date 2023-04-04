package bastoni.frontend.components

import bastoni.domain.model.GameType
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import japgolly.scalajs.react.ScalaComponent

object MainComponent:
  private val component =
    ScalaComponent
      .builder[Unit]
      .stateless
      .renderBackend[Backend]
      .build

  class Backend($ : BackendScope[Unit, Unit]):
    def render(): VdomNode = GameComponent(GameType.Briscola)

  def apply(): VdomNode = component()
