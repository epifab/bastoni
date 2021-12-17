package bastoni.domain.logic

import bastoni.domain.model.{Command, Delayed, PotentiallyDelayed, ServerEvent}
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*

type StateMachineInput = ServerEvent | Command
type StateMachineOutput = ServerEvent | Command | Delayed[Command]

trait StateMachine[T <: StateMachine[T]]
  extends (StateMachineInput => (Option[T], List[StateMachineOutput]))

object StateMachineCodecs:
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
