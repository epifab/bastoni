package bastoni.domain.logic

import bastoni.domain.ai.DumbPlayer
import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.PlayerState.*
import cats.effect.*
import cats.effect.std.Queue
import fs2.concurrent.Topic
import io.circe.{Codec, Decoder, Encoder, Printer}
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.*

import java.io.{File, PrintWriter}
import scala.concurrent.duration.DurationInt

case class RegressionSpecContent(
    users: List[User],
    input: List[StateMachineInput],
    output: List[StateMachineOutput]
)

object RegressionSpecContent:
  import StateMachineInput.{decoder, encoder}
  import StateMachineOutput.{decoder, encoder}
  given Codec[RegressionSpecContent] = deriveCodec

object RegressionSpecGen extends IOApp:
  def users(number: 2 | 3 | 4): List[User] = number match
    case 2 => List(user1, user2)
    case 3 => List(user1, user2, user3)
    case 4 => List(user1, user2, user3, user4)

  def gameIO[GameState](
      gameLogic: GameLogic[GameState],
      users: List[User]
  ): IO[(List[StateMachineInput], List[StateMachineOutput])] =
    for
      inputRef  <- Ref.of[IO, List[StateMachineInput]](Nil)
      outputRef <- Ref.of[IO, List[StateMachineOutput]](Nil)
      _ <- (for
        inputBus  <- fs2.Stream.eval(InMemoryBus[IO, StateMachineInput])
        outputBus <- fs2.Stream.eval(InMemoryBus[IO, StateMachineOutput])

        initialRoom = RoomServerView(
          seats =
            users.zipWithIndex.map { case (u, index) => TakenSeat(index, WaitingPlayer(MatchPlayer(u, 0)), Nil, Nil) },
          deck = Nil,
          board = Nil,
          matchInfo = None,
          dealerIndex = None,
          players = users.map(user => user.id -> user).toMap
        )

        inputPublisher <-
          fs2.Stream
            .resource(outputBus.subscribeAwait)
            .map(
              _
                // .evalTap(x => IO(println(x.getClass.getSimpleName)))
                .zipWithScan[RoomServerView](initialRoom) {
                  case (room, event: ServerEvent)   => room.update(event)
                  case (room, command: Command.Act) => room.withRequest(command)
                  case (room, _)                    => room
                }
                .collect {
                  case (Command.Continue, _) =>
                    Command.Continue

                  case (Delayed(Command.Continue, _), _) =>
                    Command.Continue

                  case (Command.Act(playerId, action, _), room) =>
                    val playerRoom = room.toPlayerView(playerId)
                    val seat = playerRoom.seatFor(playerId).getOrElse(throw IllegalStateException("Player not there"))
                    val user: User = users.find(_.is(playerId)).getOrElse(throw IllegalStateException("Unknown player"))
                    GameController.buildCommand(user)(DumbPlayer.act(playerRoom, seat, action) -> 0)
                }
                .through(inputBus.publish)
            )

        outputPublisher <-
          fs2.Stream
            .resource(inputBus.subscribeAwait)
            .map(
              _
                // .evalTap(x => IO(println(x.getClass.getSimpleName)))
                .evalTap(x => inputRef.update(_ :+ x))
                .through(gameLogic.playStream(users))
                .takeThrough {
                  case Command.Act(_, Action.ShuffleDeck, _)          => false
                  case _: (Event.MatchAborted | Event.MatchCompleted) => false
                  case _                                              => true
                }
                .evalTap(x => outputRef.update(_ :+ x))
                .through(outputBus.publish)
            )

        shufflingDeck = fs2.Stream(Command.ShuffleDeck(shuffleSeed)).through(inputBus.publish)

        done <- outputPublisher
          .concurrently(inputPublisher)
          .concurrently(shufflingDeck)
          .concurrently(inputBus.run)
          .concurrently(outputBus.run)
      yield ()).compile.drain

      input  <- inputRef.get
      output <- outputRef.get
    yield (input, output)

  val logicFor: Map[GameType, GameLogic[_]] = Map(
    GameType.Briscola  -> briscola.BriscolaGame,
    GameType.Tressette -> tressette.TressetteGame,
    GameType.Scopa     -> scopa.ScopaGame
  )

  def output(gameType: GameType, players: List[User]): IO[Unit] =
    gameIO(logicFor(gameType), players).flatMap { case (input, output) =>
      val body: String = RegressionSpecContent(
        players,
        input,
        output
      ).asJson.printWith(Printer.spaces2)

      val fileName =
        s"modules/domain/src/test/resources/${gameType.toString.toLowerCase}-${players.size}-players-spec.json"

      val writerIO = IO(new PrintWriter(new File(fileName)))
      val resource = Resource.make(writerIO)(writer => IO(writer.close()))

      IO(println(s"Generating $fileName")) *> resource.use { writer => IO(writer.write(body)) }
    }

  override def run(args: List[String]): IO[ExitCode] =
    for
      _ <- output(GameType.Briscola, users(2))
      _ <- output(GameType.Briscola, users(3))
      _ <- output(GameType.Briscola, users(4))
      _ <- output(GameType.Tressette, users(2))
      _ <- output(GameType.Tressette, users(3))
      _ <- output(GameType.Tressette, users(4))
      _ <- output(GameType.Scopa, users(2))
      _ <- output(GameType.Scopa, users(3))
      _ <- output(GameType.Scopa, users(4))
    yield ExitCode.Success
end RegressionSpecGen
