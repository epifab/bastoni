package bastoni.frontend
package components

import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.frontend.model.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import konva.Konva
import org.scalajs.dom.window
import reactkonva.{KCircle, KGroup, KLayer, KText}


object CardsLayer:

  case class Props(current: List[CardLayout], previous: Map[CardId, CardLayout], selectable: Map[CardId, Callback], selected: Set[CardId])

  class Backend($: BackendScope[Props, Unit]):

    def render(props: Props): VdomNode = {
      val renderedCards: List[VdomElement] =
        props
          .current
          .map(layout => CardComponent(
            layout = layout,
            previous = props.previous.get(layout.card.ref),
            selectable = props.selectable.get(layout.card.ref),
            selected = props.selected.contains(layout.card.ref)
          ))

      KLayer(renderedCards: _*)
    }

  private val component =
    ScalaComponent
      .builder[Props]
      .stateless
      .renderBackend[Backend]
      .shouldComponentUpdate(c => CallbackTo {
        val layoutHasChanged = c.currentProps.current != c.nextProps.current
        val selectableHasChanged = c.currentProps.selectable.keys.toSet != c.nextProps.selectable.keys.toSet
        val selectedHasCHanged = c.currentProps.selected != c.nextProps.selected
        layoutHasChanged || selectableHasChanged
      })
      .build

  def apply(current: List[CardLayout], previous: Map[CardId, CardLayout], selectable: Map[CardId, Callback], selected: Set[CardId]): VdomNode =
    component(Props(current, previous, selectable, selected))
