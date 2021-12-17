package bastoni.frontend

import bastoni.domain.model.GameType
import bastoni.frontend.components.GameComponent
import org.scalajs.dom.document

@main def run(): Unit =
  val root = document.getElementById("app-wrapper")
  components.GameComponent(GameType.Scopa).renderIntoDOM(root)
