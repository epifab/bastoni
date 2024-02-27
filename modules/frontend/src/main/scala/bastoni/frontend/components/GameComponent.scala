package bastoni.frontend
package components
import bastoni.domain.ai
import bastoni.domain.ai.{GreedyPlayer, VirtualPlayer}
import bastoni.domain.logic.Services
import bastoni.domain.model.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import bastoni.domain.view.FromPlayer.GameCommand
import bastoni.frontend.model.FourPlayersLayout
import bastoni.frontend.ConsoleLogger.given
import cats.effect.unsafe.implicits.global
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom
import org.scalajs.dom.{console, window}
import org.typelevel.log4cats.Logger
import reactkonva.{KLayer, KStage}

import scala.concurrent.duration.DurationInt

extension [T](io: IO[T])
  def toCallback: Callback = Callback(io.unsafeRunAsync {
    case Right(_)    => ()
    case Left(error) => console.error(error.getMessage)
  })

extension (callback: Callback) def toIO: IO[Unit] = IO(callback.runNow())

object GameComponent:

  case class State(gameState: Option[GameState], currentLayout: FourPlayersLayout, previousLayout: FourPlayersLayout):
    def refreshLayout: State =
      copy(currentLayout = FourPlayersLayout.fromWindow(gameState.map(_.currentRoom)), previousLayout = currentLayout)
    def update(nextGameState: GameState): State = copy(gameState = Some(nextGameState)).refreshLayout

  object State:
    def initial: State =
      val layout = FourPlayersLayout.fromWindow(None)
      State(None, layout, layout)

  private val component =
    ScalaComponent
      .builder[GameType]
      .initialState[State](State.initial)
      .renderBackend[GameComponentBackend]
      .componentDidMount(_.backend.start)
      .build

  def apply(gameType: GameType): VdomElement = component(gameType)

  private class GameComponentBackend($ : BackendScope[GameType, State]):
    def render(state: State): VdomNode = state.gameState match
      case Some(gameState) =>
        <.div(
          GameScoreDiv(gameState),
          KStage(
            { p =>
              p.width = window.innerWidth
              p.height = window.innerHeight
            },
            TableLayer(state.currentLayout.table),
            CardsLayerWrapper(gameState, state.currentLayout, state.previousLayout),
            DeckCountLayer(gameState, state.currentLayout.deck),
            DeckShufflingLayer(gameState.currentRoom, state.currentLayout, gameState.sendMessage),
            KLayer(
              List(
                gameState.currentRoom.mainPlayer.map(seat =>
                  PlayerComponent(
                    seat.occupant,
                    state.currentLayout.mainPlayer,
                    gameState.currentRoom.dealerIndex.contains(seat.index)
                  )
                ),
                gameState.currentRoom.opponentLeft.map(seat =>
                  PlayerComponent(
                    seat.occupant,
                    state.currentLayout.player1,
                    gameState.currentRoom.dealerIndex.contains(seat.index)
                  )
                ),
                gameState.currentRoom.opponentFront.map(seat =>
                  PlayerComponent(
                    seat.occupant,
                    state.currentLayout.player2,
                    gameState.currentRoom.dealerIndex.contains(seat.index)
                  )
                ),
                gameState.currentRoom.opponentRight.map(seat =>
                  PlayerComponent(
                    seat.occupant,
                    state.currentLayout.player3,
                    gameState.currentRoom.dealerIndex.contains(seat.index)
                  )
                )
              ).flatten: _*
            )
          )
        )

      case None => <.h1(<.img(^.src := "static/denari.svg", ^.className := "spinning main-logo"))

    val start: Callback =
      for
        gameType <- $.props
        _        <- Callback(window.onresize = _ => $.modState(_.refreshLayout).runNow())
        _        <- program(gameType).toCallback
      yield ()

    private def program(gameType: GameType): IO[Unit] = (for
      backend <- fs2.Stream.resource(Services.inMemory[IO])
      (controller, runner) = backend
      roomId               = RoomId.newId

      virtualPlayer = VirtualPlayer(controller, GreedyPlayer, pause = 1.second)
      me            = User(UserId.newId, "ME")
      p1            = virtualPlayer.play(User(UserId.newId, "Tizio"), roomId)
      p2            = virtualPlayer.play(User(UserId.newId, "Caio"), roomId)
      p3            = virtualPlayer.play(User(UserId.newId, "Sempronio"), roomId)

      rooms <- controller
        .subscribe(me, roomId)
        .scan[Option[RoomPlayerView]](None) {
          case (_, ToPlayer.Connected(room))      => Some(room)
          case (_, ToPlayer.Disconnected(_))      => None
          case (props, ToPlayer.Request(command)) => props.map(_.withRequest(command))
          case (props, ToPlayer.GameEvent(event)) => props.map(_.update(event))
          case (props, ToPlayer.Authenticated(_)) => props
          case (props, ToPlayer.Ping)             => props
        }
        .zipWithPrevious
        .collect {
          case (prev, Some(current)) if !prev.flatten.contains(current) =>
            GameState(
              current,
              prev.flatten,
              msg => controller.publish(me, roomId)(fs2.Stream(msg)).compile.drain.toCallback
            )
        }
        .evalMap(gameState => $.modState(_.update(gameState)).toIO)
        .concurrently(runner)
        .concurrently(p1) // .concurrently(p2).concurrently(p3)
        .concurrently(
          controller.publish(me, roomId)(
            fs2.Stream[IO, GameCommand](FromPlayer.Connect, FromPlayer.JoinTable).delayBy(1.second) ++
              fs2.Stream.awakeEvery[IO](2.seconds).map(_ => FromPlayer.StartMatch(gameType))
          )
        )
    yield rooms).compile.drain
  end GameComponentBackend
end GameComponent
