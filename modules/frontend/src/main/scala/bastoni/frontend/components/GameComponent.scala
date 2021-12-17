package bastoni.frontend.components

import bastoni.domain.DumbPlayer
import bastoni.domain.logic.Services
import bastoni.domain.model.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom.Console

import scala.concurrent.duration.DurationInt

val GameComponent =
  ScalaComponent
    .builder[Unit]
    .initialState[Option[PlayerTableView]](None)
    .renderBackend[GameComponentBackend]
    .componentDidMount(_.backend.start)
    .build


class GameComponentBackend($: BackendScope[Unit, Option[PlayerTableView]]):
  def render(table: Option[PlayerTableView]) = table match
    case Some(table) => <.div(^.className := "game", TableComponent(table))
    case None => <.div("Waiting...")

  val start: Callback =
    val program: IO[Unit] = (for {
      backend <- fs2.Stream.resource(Services.inMemory[IO])
      (pub, sub, runner) = backend
      room = RoomId.newId

      me = Player(PlayerId.newId, "ME")
      p1 = DumbPlayer(Player(PlayerId.newId, "Tizio"), room, sub, pub, pause = 1.second)
      p2 = DumbPlayer(Player(PlayerId.newId, "Caio"), room, sub, pub, pause = 1.second)
      p3 = DumbPlayer(Player(PlayerId.newId, "Sempronio"), room, sub, pub, pause = 1.second)
      p4 = DumbPlayer(me, room, sub, pub, pause = 1.second)

      tables <- sub.subscribe(me, room).map { case ToPlayer.Snapshot(table) => table }
        .evalMap(table => IO($.setState(Some(table)).runNow()))
        .concurrently(runner)
        .concurrently(p1).concurrently(p2).concurrently(p3).concurrently(p4)
        .concurrently(pub.publish(me, room)(fs2.Stream[IO, FromPlayer](FromPlayer.ActivateRoom(GameType.Briscola)).delayBy(2.seconds)))
    } yield tables).compile.drain

    Callback(program.unsafeRunAsync {
      case Right(_) => Console.println("Game completed successfully")
      case Left(error) => Console.err.println(s"Game crashed: ${error.getMessage}")
    })
