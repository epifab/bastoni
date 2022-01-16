package bastoni.frontend

import japgolly.scalajs.react.callback.Callback
import org.scalajs.dom.window

import scala.concurrent.duration.FiniteDuration

object Utils:

  def timeoutCallback(callback: Callback, timeout: FiniteDuration): Callback =
    Callback(window.setTimeout(() => callback.runNow(), timeout.toMillis))
