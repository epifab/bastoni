package bastoni.frontend

import bastoni.domain.model.GameType
import bastoni.frontend.components.{CardComponent, GameComponent}
import org.scalajs.dom.document

@main def run(): Unit =
  val root = document.getElementById("app-wrapper")

  // this is to pre-load all images. surely there's a better way
  CardComponent.cardImages.foreach(_._2.onload = _ => ())
  CardComponent.backOfCardImagePattern.onload = _ => ()

  components.GameComponent(GameType.Briscola).renderIntoDOM(root)
