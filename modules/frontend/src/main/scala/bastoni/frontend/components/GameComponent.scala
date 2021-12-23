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
    .builder[GameType]
    .initialState[Option[GameProps]](None)
    .renderBackend[GameComponentBackend]
    .componentDidMount(_.backend.start)
    .build


class GameComponentBackend($: BackendScope[GameType, Option[GameProps]]):
  def render(props: Option[GameProps]) = props match
    case Some(props) =>
      <.div(^.className := ("game" :: props.table.active.map(_.toString.toLowerCase).toList).mkString(" "),
        TableComponent(props)
      )
    case None => <.div("Waiting...")

  val start: Callback =
    for {
      gameType <- $.props
      _ <- Callback(program(gameType).unsafeRunAsync {
        case Right(_) => Console.println("Game completed successfully")
        case Left(error) => Console.err.println(s"Game crashed: ${error.getMessage}")
      })
    } yield ()

  private def program(gameType: GameType): IO[Unit] = (for {
    backend <- fs2.Stream.resource(Services.inMemory[IO])
    (pub, sub, runner) = backend
    roomId = RoomId.newId

    me = User(UserId.newId, "ME")
    p1 = DumbPlayer(User(UserId.newId, "Tizio"), roomId, sub, pub, pause = 1.second)
    p2 = DumbPlayer(User(UserId.newId, "Caio"), roomId, sub, pub, pause = 1.second)
    p3 = DumbPlayer(User(UserId.newId, "Sempronio"), roomId, sub, pub, pause = 1.second)
    p4 = DumbPlayer(me, roomId, sub, pub, pause = 1.second)

    tables <- sub.subscribe(me, roomId)
      .scan[Option[TablePlayerView]](None) {
        case (_, ToPlayer.Snapshot(table)) => Some(table)
        case (oldTable, ToPlayer.GameEvent(event)) => oldTable.map(_.update(event))
      }
      .collect { case Some(table) => table }
      .evalMap(table => IO($.setState(Some(GameProps(table, me.id))).runNow()))
      .concurrently(runner)
      .concurrently(p1).concurrently(p2).concurrently(p3).concurrently(p4)
      .concurrently(pub.publish(me, roomId)(fs2.Stream[IO, FromPlayer](FromPlayer.StartGame(gameType)).delayBy(2.seconds)))
  } yield tables).compile.drain
