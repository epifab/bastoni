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
    case Right(_) => ()
    case Left(error) => console.error(error.getMessage)
  })

extension(callback: Callback)
  def toIO: IO[Unit] = IO(callback.runNow())

object GameComponent:
  case class State(gameState: Option[GameState], currentLayout: GameLayout, previousLayout: GameLayout):
    def refreshLayout: State = copy(currentLayout = GameLayout.fromWindow(gameState.map(_.currentTable)), previousLayout = currentLayout)
    def update(nextGameState: GameState): State = copy(gameState = Some(nextGameState)).refreshLayout

  object State:
    def initial: State =
      val layout = GameLayout.fromWindow(None)
      State(None, layout, layout)

  private val component =
    ScalaComponent
      .builder[GameType]
      .initialState[State](State.initial)
      .renderBackend[GameComponentBackend]
      .componentDidMount(_.backend.start)
      .build

  def apply(gameType: GameType): VdomElement = component(gameType)

  private class GameComponentBackend($: BackendScope[GameType, State]):
    def render(state: State): VdomNode = state.gameState match
      case Some(gameState) =>
        KStage(
          { p =>
            p.width = window.innerWidth
            p.height = window.innerHeight
          },
          TableLayer(state.currentLayout.table),
          CardsLayerWrapper(gameState, state.currentLayout, state.previousLayout),
          DeckCountLayer(gameState, state.currentLayout.deck),
          DeckShufflingLayer(gameState.currentTable, state.currentLayout, gameState.sendMessage),
          KLayer(
            List(
              gameState.currentTable.mainPlayer.map(seat => PlayerComponent(seat.player, state.currentLayout.mainPlayer, gameState.currentTable.dealerIndex.contains(seat.index))),
              gameState.currentTable.opponent1.map(seat => PlayerComponent(seat.player, state.currentLayout.player1, gameState.currentTable.dealerIndex.contains(seat.index))),
              gameState.currentTable.opponent2.map(seat => PlayerComponent(seat.player, state.currentLayout.player2, gameState.currentTable.dealerIndex.contains(seat.index))),
              gameState.currentTable.opponent3.map(seat => PlayerComponent(seat.player, state.currentLayout.player3, gameState.currentTable.dealerIndex.contains(seat.index))),
            ).flatten: _*
          )
        )

      case None => <.div("Waiting...")

    val start: Callback =
      for {
        gameType <- $.props
        _ <- Callback(window.onresize = _ => $.modState(_.refreshLayout).runNow())
        _ <- program(gameType).toCallback
      } yield ()

    private def program(gameType: GameType): IO[Unit] = (for {
      backend <- fs2.Stream.resource(Services.inMemory[IO])
      (pub, sub, runner) = backend
      roomId = RoomId.newId

      me = User(UserId.newId, "ME")
      p1 = DumbPlayer(User(UserId.newId, "Tizio"), roomId, sub, pub, pause = 1.second)
      p2 = DumbPlayer(User(UserId.newId, "Caio"), roomId, sub, pub, pause = 1.second)
      p3 = DumbPlayer(User(UserId.newId, "Sempronio"), roomId, sub, pub, pause = 1.second)

      tables <- sub.subscribe(me, roomId)
        .scan[Option[TablePlayerView]](None) {
          case (_, ToPlayer.Snapshot(table)) => Some(table)
          case (props, ToPlayer.GameEvent(event)) => props.map(_.update(event))
        }
        .zipWithPrevious
        .collect { case (prev, Some(current)) if !prev.flatten.contains(current) =>
          GameState(
            me.id,
            current,
            prev.flatten,
            msg => pub.publish(me, roomId)(fs2.Stream(msg)).compile.drain.toCallback
          )
        }
        .evalMap(gameState => $.modState(_.update(gameState)).toIO)
        .concurrently(runner)
        .concurrently(p1).concurrently(p2).concurrently(p3)
        .concurrently(pub.publish(me, roomId)(
          fs2.Stream[IO, FromPlayer](FromPlayer.Connect, FromPlayer.JoinTable).delayBy(1.second) ++
            fs2.Stream.awakeEvery[IO](2.seconds).map(_ => FromPlayer.StartMatch(gameType))
        ))
    } yield tables).compile.drain
