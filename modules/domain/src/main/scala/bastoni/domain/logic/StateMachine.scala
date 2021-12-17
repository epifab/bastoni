package bastoni.domain.logic

import bastoni.domain.model.{Command, Delayed, PotentiallyDelayed, ServerEvent}
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*

type StateMachineInput = ServerEvent | Command
type StateMachineOutput = ServerEvent | Command | Delayed[Command]

trait StateMachine[T <: StateMachine[T]]
  extends (StateMachineInput => (Option[T], List[StateMachineOutput]))
