package bastoni.domain.logic

import bastoni.domain.model.*
import cats.Show
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.syntax.*

type StateMachineInput  = ServerEvent | Command
type StateMachineOutput = ServerEvent | Command | Delayed[Command]

trait StateMachine[T <: StateMachine[T]] extends (StateMachineInput => (Option[T], List[StateMachineOutput]))

object StateMachineInput:
  given encoder: Encoder[StateMachineInput] = Encoder.instance {
    case e: ServerEvent => Encoder[ServerEvent].apply(e).mapObject(_.add("supertype", "Event".asJson))
    case c: Command     => Encoder[Command].apply(c).mapObject(_.add("supertype", "Command".asJson))
  }

  given decoder: Decoder[StateMachineInput] = Decoder.instance { obj =>
    val typeCursor = obj.downField("supertype")

    typeCursor.as[String].flatMap {
      case "Event"   => Decoder[ServerEvent].tryDecode(obj)
      case "Command" => Decoder[Command].tryDecode(obj)
      case supertype => Left(DecodingFailure(s"Not an event nor a command: $supertype", typeCursor.history))
    }
  }

  given Show[StateMachineInput] = Show {
    case serverEvent: ServerEvent => Show[ServerEvent].show(serverEvent)
    case command: Command         => Show[Command].show(command)
  }

object StateMachineOutput:
  given encoder: Encoder[StateMachineOutput] = Encoder.instance {
    case e: ServerEvent => Encoder[ServerEvent].apply(e).mapObject(_.add("supertype", "Event".asJson))
    case c: Command     => Encoder[Command].apply(c).mapObject(_.add("supertype", "Command".asJson))
    case Delayed(c: Command, delay) =>
      Encoder[Command].apply(c).mapObject(_.add("supertype", "Command".asJson).add("delay", delay.asJson))
  }

  given decoder: Decoder[StateMachineOutput] = Decoder.instance { obj =>
    val typeCursor = obj.downField("supertype")

    typeCursor.as[String].flatMap {
      case "Event" => Decoder[ServerEvent].tryDecode(obj)
      case "Command" =>
        Decoder[Command].tryDecode(obj).flatMap { command =>
          obj.downField("delay").as[Option[Delay]].map {
            case Some(delay) => Delayed(command, delay)
            case None        => command
          }
        }
      case supertype => Left(DecodingFailure(s"Not an event nor a command: $supertype", typeCursor.history))
    }
  }

  given Show[StateMachineOutput] = Show {
    case serverEvent: ServerEvent         => Show[ServerEvent].show(serverEvent)
    case command: Command                 => Show[Command].show(command)
    case delayedCommand: Delayed[Command] => Show[Delayed[Command]].show(delayedCommand)
  }

end StateMachineOutput
