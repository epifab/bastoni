package bastoni.domain.logic

import bastoni.domain.model.{Command, Delayed, ServerEvent}

trait StateMachine[T <: StateMachine[T]]
  extends ((ServerEvent | Command) => (Option[T], List[ServerEvent | Command | Delayed[Command]]))
