package bastoni.sdk

import bastoni.domain.ai.{GreedyPlayer, VirtualPlayer}
import bastoni.domain.logic.Services
import bastoni.domain.model.*
import bastoni.domain.view.*
import bastoni.sdk.ConsoleLogger.given
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import io.circe
import io.circe.parser.decode
import io.circe.syntax.EncoderOps

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

trait GameSdk:
  def sendMessage(msg: FromPlayer): Unit

object GameSdk:

  given IORuntime = cats.effect.unsafe.IORuntime.global

  type FromPlayerJs = js.Dynamic

  object FromPlayerJs:
    def parse(msg: FromPlayerJs): IO[FromPlayer] =
      IO.fromEither(decode(js.JSON.stringify(msg)))

  type ToPlayerJs = js.Dynamic

  object ToPlayerJs:
    def apply(msg: ToPlayer): ToPlayerJs =
      val json = msg.asJson.noSpaces
      js.JSON.parse(json)

  type RoomJs = js.UndefOr[js.Dynamic]

  object RoomJs:
    def apply(msg: Option[RoomPlayerView]): RoomJs =
      val json = msg.asJson.noSpaces
      js.JSON.parse(json)

  @JSExportTopLevel("playAgainstComputer")
  def playAgainstComputer(
      playerName: String,
      gameType: String,
      onMessage: js.Function2[ToPlayerJs, RoomJs, Unit],
      onInit: js.Function1[js.Function1[FromPlayerJs, Unit], Unit]
  ): js.Function0[Unit] =
    val program: IO[Unit] = Services
      .inMemory[IO]
      .map { case (controller, runner) =>
        val roomId        = RoomId.newId
        val me            = User(UserId.newId, playerName)
        val virtualPlayer = VirtualPlayer(controller, GreedyPlayer, pause = 1.second)
        val opponent      = virtualPlayer.play(User(UserId.newId, "Tony"), roomId)
        val connectMe = controller.publish(me, roomId)(
          fs2.Stream[IO, FromPlayer](FromPlayer.Connect, FromPlayer.JoinTable) ++
            fs2.Stream.awakeEvery[IO](2.seconds).map(_ => FromPlayer.StartMatch(GameType.valueOf(gameType)))
        )
        val bg = controller
          .connectPlayer(me, roomId)
          .evalMap { (msg, room) => IO(onMessage(ToPlayerJs(msg), RoomJs(room))) }
          .concurrently(runner)
          .concurrently(connectMe)
          .concurrently(opponent)
        val control = (message: FromPlayer) => controller.publish1(me, roomId)(message)
        control -> bg
      }
      .use { case (control, stream) =>
        IO(onInit { (gameCommand: FromPlayerJs) =>
          FromPlayerJs
            .parse(gameCommand)
            .flatMap(control.apply)
            .unsafeRunAndForget()
        }) *> stream.compile.drain
      }

    val cancel = program.unsafeRunCancelable()
    () => cancel()
  end playAgainstComputer

end GameSdk
