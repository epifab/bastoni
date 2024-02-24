package bastoni.sdk

import bastoni.domain.ai.{GreedyPlayer, VirtualPlayer}
import bastoni.domain.logic.Services
import bastoni.domain.model.*
import bastoni.domain.view.*
import bastoni.domain.view.FromPlayer.GameCommand
import bastoni.sdk.ConsoleLogger.given
import cats.effect.unsafe.IORuntime
import cats.effect.IO
import io.circe
import io.circe.parser.decode
import io.circe.syntax.EncoderOps

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

trait GameSdk:
  def sendMessage(msg: FromPlayer): Unit

object GameSdk:

  given IORuntime = cats.effect.unsafe.IORuntime.global

  type GameCommandJs = js.Dynamic

  object GameCommandJs:
    def parse(msg: GameCommandJs): IO[GameCommand] =
      IO.fromEither(decode(js.JSON.stringify(msg)))

  type ToPlayerJs = js.Dynamic

  object ToPlayerJs:
    def apply(msg: ToPlayer): ToPlayerJs =
      val json = msg.asJson.noSpaces
      js.JSON.parse(json)

  @JSExportTopLevel("playAgainstComputer")
  def playAgainstComputer(
      playerName: String,
      gameType: String,
      callback: js.Function1[ToPlayerJs, Unit],
      onInit: js.Function1[js.Function1[GameCommandJs, Unit], Unit]
  ): Unit =
    Services
      .inMemory[IO]
      .map { case (controller, runner) =>
        val roomId        = RoomId.newId
        val me            = User(UserId.newId, playerName)
        val virtualPlayer = VirtualPlayer(controller, GreedyPlayer, pause = 1.second)
        val opponent      = virtualPlayer.play(User(UserId.newId, "Tony"), roomId)
        val bg = controller
          .subscribe(me, roomId)
          .evalMap(msg => IO(callback(ToPlayerJs(msg))))
          .concurrently(runner)
          .concurrently(opponent)
          .concurrently(
            controller.publish(me, roomId)(
              fs2.Stream[IO, GameCommand](FromPlayer.Connect, FromPlayer.JoinTable).delayBy(1.second) ++
                fs2.Stream.awakeEvery[IO](2.seconds).map(_ => FromPlayer.StartMatch(GameType.valueOf(gameType)))
            )
          )
        val sdk = new GameSdk:
          override def sendMessage(message: FromPlayer): Unit =
            controller.publish1(me, roomId)(message).unsafeRunAndForget()
        sdk -> bg
      }
      .use { case (control, stream) =>
        IO(onInit { (gameCommand: GameCommandJs) =>
          GameCommandJs.parse(gameCommand)
        }) *> stream.compile.drain
      }
      .unsafeRunAndForget()

end GameSdk
