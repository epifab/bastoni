package reactkonva

import bastoni.frontend.JsObject
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.{TagMod, VdomElement, VdomNode}
import konva.*
import konva.Konva.Vector2d
import org.scalajs.dom.*
import reactkonva.ReactKonvaDOM.Context

import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation.JSGlobal

trait ExternalComponent[Props <: NodeProps]:
  def apply(f: Props => Unit): TagMod

trait ExternalComponentWithoutChildren[Props <: ShapeProps](rawComponent: js.Object):
  private val component = JsComponent[Props, Children.None, Null](rawComponent)

  def apply(props: Props): VdomNode     = component(props)
  def apply(f: Props => Unit): VdomNode = component(JsObject[Props](f))

trait ExternalComponentWithChildren[Props <: ContainerProps](rawComponent: js.Object):
  private val component = JsComponent[Props, Children.Varargs, Null](rawComponent)

  def apply(props: Props, children: VdomNode*): VdomNode     = component(props)(children: _*)
  def apply(f: Props => Unit, children: VdomNode*): VdomNode = component(JsObject[Props](f))(children: _*)
  def apply(children: VdomNode*): VdomNode                   = component(JsObject[Props](_ => ()))(children: _*)
