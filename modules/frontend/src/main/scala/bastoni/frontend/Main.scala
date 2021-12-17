package bastoni.frontend

import bastoni.frontend.components.GameComponent
import org.scalajs.dom.document

@main def run(): Unit =
  val root = document.getElementById("app-wrapper")
  components.GameComponent().renderIntoDOM(root)
