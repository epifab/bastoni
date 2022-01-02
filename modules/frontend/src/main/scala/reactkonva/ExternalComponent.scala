package reactkonva

import japgolly.scalajs.react.{Children, CtorType, JsComponent}
import japgolly.scalajs.react.vdom.{TagMod, VdomElement, VdomNode}
import konva.{Konva, ShapeRef}
import konva.Konva.Vector2d
import org.scalajs.dom.*
import reactkonva.ReactKonvaDOM.Context

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.|

@js.native
trait NodeProps extends js.Object:
  // Node
  var x: js.UndefOr[Double] = js.native
  var y: js.UndefOr[Double] = js.native
  var width: js.UndefOr[Double] = js.native
  var height: js.UndefOr[Double] = js.native
  var visible: js.UndefOr[Boolean] = js.native
  var listening: js.UndefOr[Boolean] = js.native
  var id: js.UndefOr[String] = js.native
  var name: js.UndefOr[String] = js.native
  var opacity: js.UndefOr[Double] = js.native
  var scale: js.UndefOr[Vector2d] = js.native
  var scaleX: js.UndefOr[Double] = js.native
  var scaleY: js.UndefOr[Double] = js.native
  var rotation: js.UndefOr[Int] = js.native
  var offset: js.UndefOr[Vector2d] = js.native
  var offsetX: js.UndefOr[Int] = js.native
  var offsetY: js.UndefOr[Int] = js.native

  var _useStrictMode: js.UndefOr[Boolean] = js.native
  var draggable: js.UndefOr[Boolean] = js.native

  var onMouseOver: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onMouseMove: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onMouseOut: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onMouseEnter: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onMouseLeave: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onMouseDown: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onMouseUp: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onWheel: js.UndefOr[js.Function1[Konva.KonvaEventObject[WheelEvent], Unit]] = js.native
  var onClick: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onDblClick: js.UndefOr[js.Function1[Konva.KonvaEventObject[MouseEvent], Unit]] = js.native
  var onTouchStart: js.UndefOr[js.Function1[Konva.KonvaEventObject[TouchEvent], Unit]] = js.native
  var onTouchMove: js.UndefOr[js.Function1[Konva.KonvaEventObject[TouchEvent], Unit]] = js.native
  var onTouchEnd: js.UndefOr[js.Function1[Konva.KonvaEventObject[TouchEvent], Unit]] = js.native
  var onTap: js.UndefOr[js.Function1[Konva.KonvaEventObject[Event], Unit]] = js.native
  var onDblTap: js.UndefOr[js.Function1[Konva.KonvaEventObject[Event], Unit]] = js.native
  var onDragStart: js.UndefOr[js.Function1[Konva.KonvaEventObject[DragEvent], Unit]] = js.native
  var onDragMove: js.UndefOr[js.Function1[Konva.KonvaEventObject[DragEvent], Unit]] = js.native
  var onDragEnd: js.UndefOr[js.Function1[Konva.KonvaEventObject[DragEvent], Unit]] = js.native
  var onTransform: js.UndefOr[js.Function1[Konva.KonvaEventObject[Event], Unit]] = js.native
  var onTransformStart: js.UndefOr[js.Function1[Konva.KonvaEventObject[Event], Unit]] = js.native
  var onTransformEnd: js.UndefOr[js.Function1[Konva.KonvaEventObject[Event], Unit]] = js.native
  var onContextMenu: js.UndefOr[js.Function1[Konva.KonvaEventObject[PointerEvent], Unit]] = js.native

@js.native
trait ContainerProps extends NodeProps:
  var clearBeforeDraw: js.UndefOr[Boolean] = js.native
  var clipFunc: js.UndefOr[js.Function1[Context, Unit]] = js.native
  var clipX: js.UndefOr[Double] = js.native
  var clipY: js.UndefOr[Double] = js.native
  var clipWidth: js.UndefOr[Double] = js.native
  var clipHeight: js.UndefOr[Double] = js.native

@js.native
trait ShapeProps extends NodeProps:
  // Shape
  var fill: js.UndefOr[String] = js.native
  var fillPatternImage: js.UndefOr[HTMLImageElement] = js.native
  var fillPatternX: js.UndefOr[Double] = js.native
  var fillPatternY: js.UndefOr[Double] = js.native
  var fillPatternOffset: js.UndefOr[Vector2d] = js.native
  var fillPatternOffsetX: js.UndefOr[Double] = js.native
  var fillPatternOffsetY: js.UndefOr[Double] = js.native
  var fillPatternScale: js.UndefOr[Vector2d] = js.native
  var fillPatternScaleX: js.UndefOr[Double] = js.native
  var fillPatternScaleY: js.UndefOr[Double] = js.native
  var fillPatternRotation: js.UndefOr[Double] = js.native
  var fillPatternRepeat: js.UndefOr[String] = js.native
  var fillLinearGradientStartPoint: js.UndefOr[Vector2d] = js.native
  var fillLinearGradientStartPointX: js.UndefOr[Double] = js.native
  var fillLinearGradientStartPointY: js.UndefOr[Double] = js.native
  var fillLinearGradientEndPoint: js.UndefOr[Vector2d] = js.native
  var fillLinearGradientEndPointX: js.UndefOr[Double] = js.native
  var fillLinearGradientEndPointY: js.UndefOr[Double] = js.native
  var fillLinearGradientColorStops: js.UndefOr[Seq[js.Any]] = js.native // TODO : Array<number | string>;
  var fillRadialGradientStartPoint: js.UndefOr[Vector2d] = js.native
  var fillRadialGradientStartPointX: js.UndefOr[Double] = js.native
  var fillRadialGradientStartPointY: js.UndefOr[Double] = js.native
  var fillRadialGradientEndPoint: js.UndefOr[Vector2d] = js.native
  var fillRadialGradientEndPointX: js.UndefOr[Double] = js.native
  var fillRadialGradientEndPointY: js.UndefOr[Double] = js.native
  var fillRadialGradientStartRadius: js.UndefOr[Double] = js.native
  var fillRadialGradientEndRadius: js.UndefOr[Double] = js.native
  var fillRadialGradientColorStops: js.UndefOr[Seq[js.Any]] = js.native // TODO : Array<number | string>;
  var fillEnabled: js.UndefOr[Boolean] = js.native
  var fillPriority: js.UndefOr[String] = js.native
  var stroke: js.UndefOr[String] = js.native
  var strokeWidth: js.UndefOr[Double] = js.native
  var hitStrokeWidth: js.UndefOr[Double | String] = js.native
  var strokeScaleEnabled: js.UndefOr[Boolean] = js.native
  var strokeHitEnabled: js.UndefOr[Boolean] = js.native
  var strokeEnabled: js.UndefOr[Boolean] = js.native
  var lineJoin: js.UndefOr[String] = js.native
  var lineCap: js.UndefOr[String] = js.native
  var sceneFunc: js.UndefOr[(Context, ShapeRef) => Unit] = js.native
  var hitFunc: js.UndefOr[(Context, ShapeRef) => Unit] = js.native
  var shadowColor: js.UndefOr[String] = js.native
  var shadowBlur: js.UndefOr[Double] = js.native
  var shadowOffset: js.UndefOr[Vector2d] = js.native
  var shadowOffsetX: js.UndefOr[Double] = js.native
  var shadowOffsetY: js.UndefOr[Double] = js.native
  var shadowOpacity: js.UndefOr[Double] = js.native
  var shadowEnabled: js.UndefOr[Boolean] = js.native
  var shadowForStrokeEnabled: js.UndefOr[Boolean] = js.native
  var dash: js.UndefOr[Seq[Double]] = js.native
  var dashOffset: js.UndefOr[Double] = js.native
  var dashEnabled: js.UndefOr[Boolean] = js.native
  var perfectDrawEnabled: js.UndefOr[Boolean] = js.native

trait ExternalComponent[Props <: NodeProps]:
  def apply(f: Props => Unit): TagMod

trait ExternalComponentPropsNoChildren[Props <: ShapeProps](rawComponent: js.Object):
  private val component = JsComponent[Props, Children.None, Null](rawComponent)

  def builder: ExternalComponentWithoutChildrenBuilder[Props] =
    val props = (new js.Object).asInstanceOf[Props]
    ExternalComponentWithoutChildrenBuilder(props, () => component(props))


trait ExternalComponentPropsAndChildren[Props <: ContainerProps](rawComponent: js.Object):
  private val component = JsComponent[Props, Children.Varargs, Null](rawComponent)

  def build(children: VdomNode*): VdomNode =
    builder.build(children: _*)

  def builder: ExternalComponentWithChildrenBuilder[Props] =
    val props = (new js.Object).asInstanceOf[Props]
    ExternalComponentWithChildrenBuilder(props, (children: Seq[VdomNode]) => component(props)(children: _*))

case class ExternalComponentWithoutChildrenBuilder[Props <: js.Object](props: Props, build: () => VdomNode):
  def set(f: Props => Unit): ExternalComponentWithoutChildrenBuilder[Props] =
    f(props)
    ExternalComponentWithoutChildrenBuilder(props, build)

case class ExternalComponentWithChildrenBuilder[Props <: js.Object](props: Props, private val rawBuild: Seq[VdomNode] => VdomElement):
  def set(f: Props => Unit): ExternalComponentWithChildrenBuilder[Props] =
    f(props)
    ExternalComponentWithChildrenBuilder(props, rawBuild)

  def build(children: VdomNode*): VdomNode = rawBuild(children)