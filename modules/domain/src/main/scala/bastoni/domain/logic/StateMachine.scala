package bastoni.domain.logic

import bastoni.domain.model.{Command, Delay, Delayed, PotentiallyDelayed, ServerEvent}
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.generic.semiauto.deriveEncoder
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
