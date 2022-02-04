package bastoni.domain.logic

import bastoni.domain.logic.Fixtures.*
import bastoni.domain.model.*
import bastoni.domain.model.PlayerState.*
import cats.effect.*
import cats.effect.std.Queue
import fs2.concurrent.Topic
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder, Printer}

import java.io.{File, PrintWriter}
import scala.concurrent.duration.DurationInt


case class RegressionSpecContent(
  users: List[User],
  input: List[StateMachineInput],
  output: List[StateMachineOutput]
)

object RegressionSpecContent:
  given inputEncoder: Encoder[StateMachineInput] = Encoder.instance {
    case e: ServerEvent => Encoder[ServerEvent].mapJson(_.mapObject(_.add("supertype", "Event".asJson)))(e)
    case c: Command => Encoder[Command].mapJson(_.mapObject(_.add("supertype", "Command".asJson)))(c)
  }

  given inputDecoder: Decoder[StateMachineInput] = Decoder.instance(obj => obj.downField("supertype").as[String].flatMap {
    case "Event" => Decoder[ServerEvent].tryDecode(obj)
    case "Command" => Decoder[Command].tryDecode(obj)
  })

  given outputEncoder: Encoder[StateMachineOutput] = Encoder.instance {
    case e: ServerEvent => Encoder[ServerEvent].mapJson(_.mapObject(_.add("supertype", "Event".asJson)))(e)
    case c: Command => PotentiallyDelayed.commandEncoder.mapJson(_.mapObject(_.add("supertype", "Command".asJson)))(c)
    case Delayed(c: Command, d) => PotentiallyDelayed.commandEncoder.mapJson(_.mapObject(_.add("supertype", "Command".asJson)))(Delayed(c, d))
  }

  given outputDecoder: Decoder[StateMachineOutput] = Decoder.instance(obj => obj.downField("supertype").as[String].flatMap {
    case "Event" => Decoder[ServerEvent].tryDecode(obj)
    case "Command" => PotentiallyDelayed.decoder[Command].tryDecode(obj)
  })

  given Codec[RegressionSpecContent] = deriveCodec


object RegressionSpecGen extends IOApp:
  def users(number: 2 | 3 | 4): List[User] = number match
    case 2 => List(user1, user2)
    case 3 => List(user1, user2, user3)
    case 4 => List(user1, user2, user3, user4)

  def gameIO[MatchState](
    gameLogic: GameLogic[MatchState],
    users: List[User]
  ): IO[(List[StateMachineInput], List[StateMachineOutput])] =
    for {
      inputRef <- Ref.of[IO, List[StateMachineInput]](Nil)
      outputRef <- Ref.of[IO, List[StateMachineOutput]](Nil)
      _ <- (for {
        inputBus <- fs2.Stream.eval(Fs2Bus[IO, StateMachineInput])
        outputBus <- fs2.Stream.eval(Fs2Bus[IO, StateMachineOutput])

        initialRoom = RoomServerView(
          seats = users.zipWithIndex.map { case (u, index) => TakenSeat(index, WaitingPlayer(MatchPlayer(u, 0)), Nil, Nil) },
          deck = Nil,
          board = Nil,
          matchInfo = None,
          dealerIndex = None
        )

        inputPublisher <-
          fs2.Stream.resource(outputBus.subscribeAwait).map(_
            // .evalTap(x => IO(println(x.getClass.getSimpleName)))
            .zipWithScan[RoomServerView](initialRoom) {
              case (room, event: ServerEvent) => room.update(event)
              case (room, _) => room
            }
            .collect {
              case (Command.Continue, _) =>
                Command.Continue

              case (Delayed(Command.Continue, _), _) =>
                Command.Continue

              case (Event.ActionRequested(playerId, Action.PlayCard, _), room) =>
                val card = room
                  .seatFor(playerId)
                  .flatMap(_.hand.headOption.map(_.card))
                  .getOrElse(throw IllegalStateException("Player not there or empty hand"))
                Command.PlayCard(playerId, card)

              case (Event.ActionRequested(playerId, Action.PlayCardOf(suit), _), room) =>
                val card = room
                  .seatFor(playerId)
                  .flatMap { player => player.hand.find(_.card.suit == suit).orElse(player.hand.headOption).map(_.card) }
                  .getOrElse(throw IllegalStateException("Player not there or empty hand"))
                Command.PlayCard(playerId, card)

              case (Event.ActionRequested(playerId, Action.TakeCards, _), room) =>
                val card = room
                  .seatFor(playerId)
                  .flatMap(_.hand.headOption.map(_.card))
                  .getOrElse(throw IllegalStateException("Player not there or empty hand"))
                val taken = scopa.Game.takeCombinations(room.board.map { case (_, c) => c.card }, card).next().toList
                Command.TakeCards(playerId, card, taken)
            }
            .through(inputBus.publish)
          )

        outputPublisher <-
          fs2.Stream.resource(inputBus.subscribeAwait).map(_
            // .evalTap(x => IO(println(x.getClass.getSimpleName)))
            .evalTap(x => inputRef.update(_ :+ x))
            .through(gameLogic.playStream(users))
            .takeThrough {
              case Event.ActionRequested(_, Action.ShuffleDeck, _) => false
              case Event.MatchAborted => false
              case _: Event.MatchCompleted => false
              case _ => true
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
      } yield ()).compile.drain


      input <- inputRef.get
      output <- outputRef.get

    } yield (input, output)

  val logicFor: Map[GameType, GameLogic[_]] = Map(
    GameType.Briscola -> briscola.Game,
    GameType.Tressette -> tressette.Game,
    GameType.Scopa -> scopa.Game
  )

  def output(gameType: GameType, players: List[User]): IO[Unit] =
    gameIO(logicFor(gameType), players).flatMap { case (input, output) =>
      val body: String = RegressionSpecContent(
        players,
        input,
        output
      ).asJson.printWith(Printer.spaces2)

      val fileName = s"modules/domain/src/test/resources/${gameType.toString.toLowerCase}-${players.size}-players-spec.json"

      val writerIO = IO(new PrintWriter(new File(fileName)))
      val resource = Resource.make(writerIO)(writer => IO(writer.close()))

      IO(println(s"Generating $fileName")) *> resource.use { writer => IO(writer.write(body)) }
    }

  override def run(args: List[String]): IO[ExitCode] =

    for {
      _ <- output(GameType.Briscola, users(2))
      _ <- output(GameType.Briscola, users(3))
      _ <- output(GameType.Briscola, users(4))
      _ <- output(GameType.Tressette, users(2))
      _ <- output(GameType.Tressette, users(3))
      _ <- output(GameType.Tressette, users(4))
      _ <- output(GameType.Scopa, users(2))
      _ <- output(GameType.Scopa, users(3))
      _ <- output(GameType.Scopa, users(4))
    } yield ExitCode.Success

