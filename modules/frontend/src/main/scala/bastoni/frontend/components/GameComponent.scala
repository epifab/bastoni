package bastoni.frontend
package components

import bastoni.domain.DumbPlayer
import bastoni.domain.logic.Services
import bastoni.domain.model.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import bastoni.frontend.model.{GameLayout, Size}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom
import org.scalajs.dom.html.Image
import org.scalajs.dom.{Console, HTMLImageElement, console, window}
import reactkonva.{KGroup, KLayer, KStage}

import scala.scalajs.js
import scala.concurrent.duration.DurationInt

extension[T](io: IO[T])
  def toCallback: Callback = Callback(io.unsafeRunAsync {
    case Right(_) => console.debug("IO run successfully")
    case Left(error) => console.error(error.getMessage)
  })

extension(callback: Callback)
  def toIO: IO[Unit] = IO(callback.runNow())

object GameComponent:
  private val component =
    ScalaComponent
      .builder[GameType]
      .initialState[Option[GameProps]](None)
      .renderBackend[GameComponentBackend]
      .componentDidMount(_.backend.start)
      .build

  def apply(gameType: GameType): VdomElement = component(gameType)


class GameComponentBackend($: BackendScope[GameType, Option[GameProps]]):
  val layout: GameLayout = GameLayout(Size(window.innerWidth, window.innerHeight))

  def render(state: Option[GameProps]): VdomNode = state match
    case Some(props) =>
      KStage(
        { p =>
          p.width = window.innerWidth
          p.height = window.innerHeight
        },
        KLayer(TableComponent(layout.table)),
        KLayer(CardsLayer(props, layout)),
        KLayer(
          List(
            props.currentTable.mySeat.map(_.player).map(PlayerComponent(_, layout.mainPlayer)),
            props.currentTable.opponent(0).flatMap(_.player).map(PlayerComponent(_, layout.player1)),
            props.currentTable.opponent(1).flatMap(_.player).map(PlayerComponent(_, layout.player2)),
            props.currentTable.opponent(2).flatMap(_.player).map(PlayerComponent(_, layout.player3)),
          ).flatten: _*
        )
      )

    case None => <.div("Waiting...")

  val start: Callback =
    for {
      gameType <- $.props
      _ <- program(gameType).toCallback
    } yield ()

  private def program(gameType: GameType): IO[Unit] = (for {
    backend <- fs2.Stream.resource(Services.inMemory[IO])
    (pub, sub, runner) = backend
    roomId = RoomId.newId

    me = User(UserId.newId, "ME")
    p1 = DumbPlayer(User(UserId.newId, "Tizio"), roomId, sub, pub, pause = 4.seconds)
    p2 = DumbPlayer(User(UserId.newId, "Caio"), roomId, sub, pub, pause = 4.seconds)
    p3 = DumbPlayer(User(UserId.newId, "Sempronio"), roomId, sub, pub, pause = 4.seconds)

    tables <- sub.subscribe(me, roomId)
      .scan[Option[TablePlayerView]](None) {
        case (_, ToPlayer.Snapshot(table)) => Some(table)
        case (props, ToPlayer.GameEvent(event)) => props.map(_.update(event))
      }
      .zipWithPrevious
      .collect { case (prev, Some(current)) if !prev.flatten.contains(current) =>
        GameProps(
          me.id,
          current,
          prev.flatten,
          msg => pub.publish(me, roomId)(fs2.Stream(msg)).compile.drain.toCallback
        )
      }
      .evalMap(props => $.setState(Some(props)).toIO)
      .concurrently(runner)
      .concurrently(p1).concurrently(p2).concurrently(p3)
      .concurrently(pub.publish(me, roomId)(
        fs2.Stream[IO, FromPlayer](FromPlayer.Connect, FromPlayer.JoinTable).delayBy(1.second) ++
          fs2.Stream.awakeEvery[IO](2.seconds).map(_ => FromPlayer.StartGame(gameType))
      ))
  } yield tables).compile.drain
