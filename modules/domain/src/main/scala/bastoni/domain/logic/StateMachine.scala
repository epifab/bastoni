package bastoni.domain.logic

import bastoni.domain.model.{Command, Delayed, ServerEvent}

type StateMachineInput = ServerEvent | Command
type StateMachineOutput = ServerEvent | Command | Delayed[Command]

trait StateMachine[T <: StateMachine[T]]
  extends (StateMachineInput => (Option[T], List[StateMachineOutput]))
