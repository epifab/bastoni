package bastoni.domain.logic.generic

import bastoni.domain.logic.briscola.MatchState
import bastoni.domain.logic.briscola.MatchState.{Aborted, PlayRound, WaitingForPlayer}
import bastoni.domain.model.*
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

trait Timer[State, Self <: Timer[State, Self]]:
  this: State =>
    def timeout: Timeout.Active
    def request: Event.ActionRequested
    def ref: Int

    def timedOut: State
    def update(timeout: Timeout.Active, request: Event.ActionRequested): Self & State

    def ticked(tick: Command.Tick): (State, List[ServerEvent | Delayed[Command]]) =
      if (tick.ref != ref) (this -> Nil)
      else {
        timeout.next match
          case Timeout.TimedOut =>
            timedOut -> List(Event.TimedOut(request.playerId, request.action))
          case newTimeout: Timeout.Active =>
            val updated = update(newTimeout, request.copy(timeout = Some(newTimeout)))
            updated -> List(updated.request, updated.nextTick)
      }

    def nextTick: Delayed[Command] = Delayed(Command.Tick(ref), Delay.Tick)


object Timer:
  def ref[A](a: A)(using encoder: Encoder[A]): Int =
    // one option here would be to use a.hashCode,
    // but that's probably going to return different values for different application execution
    // which would make it difficult to test. hashCode on a string seems to be consistent
    encoder(a).printWith(io.circe.Printer.noSpaces.copy(dropNullValues = true, sortKeys = true)).hashCode
