package bastoni.frontend

import scalajs.js

object JsObject:
  def apply[X <: js.Object](f: X => Unit): X =
    val obj = (new js.Object).asInstanceOf[X]
    f(obj)
    obj
