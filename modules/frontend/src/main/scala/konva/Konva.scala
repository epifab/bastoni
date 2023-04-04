package konva

import org.scalajs.dom.HTMLCanvasElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@JSImport("konva", JSImport.Default)
@js.native
object Konva extends js.Object:

  @js.native
  class Image(props: js.Object) extends js.Object

  @js.native
  class Group(props: js.Object) extends js.Object:
    def toCanvas(): HTMLCanvasElement = js.native
    def add(image: Image): Unit       = js.native

  @js.native
  object Util extends js.Object:
    def getRandomColor(): String                        = js.native
    def getRGB(color: String): RGB                      = js.native
    def haveIntersection(r1: IRect, r2: IRect): Boolean = js.native

  @js.native
  class Animation(func: js.Function1[IFrame, Unit], layers: Seq[ShapeRef]) extends js.Object:
    def start(): Unit        = js.native
    def stop(): Unit         = js.native
    def isRunning(): Boolean = js.native

  @js.native
  trait IFrame extends js.Object:
    def time: Double
    def timeDiff: Double
    def lastTime: Double
    def frameRate: Double

  @js.native
  trait Vector2d extends js.Object:
    def x: Double
    def y: Double

  @js.native
  trait IRect extends js.Object:
    def x: Double
    def y: Double
    def width: Double
    def height: Double

  @js.native
  trait RGB extends js.Object:
    def r: Int
    def g: Int
    def b: Int

  @js.native
  trait RGBA extends RGB:
    def a: Int

  @js.native
  trait KonvaEventObject[+EventType] extends js.Object:
    def target: ShapeRef
    def evt: EventType
    def currentTarget: NodeRef
    def cancelBubble: Boolean
    def child: js.UndefOr[NodeRef]
end Konva
