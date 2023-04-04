package bastoni.domain.logic
package generic

import bastoni.domain.logic.briscola.BriscolaGameState
import bastoni.domain.logic.briscola.BriscolaGameState.{Aborted, PlayRound, WaitingForPlayer}
import bastoni.domain.model.*
import io.circe.{Encoder, Printer}
import io.circe.generic.semiauto.deriveEncoder

trait Timer[State, Self <: Timer[State, Self]]:
  this: State =>
  def timeout: Timeout.Active
  def request: Command.Act
  def ref: Int

  def timedOut: State
  def update(timeout: Timeout.Active, request: Command.Act): Self & State

  def ticked(tick: Command.Tick): (State, List[StateMachineOutput]) =
    if (tick.ref != ref) (this -> Nil)
    else
      timeout.next match
        case Timeout.TimedOut =>
          timedOut -> List(Event.TimedOut(request.playerId, request.action))
        case newTimeout: Timeout.Active =>
          val updated = update(newTimeout, request.copy(timeout = Some(newTimeout)))
          updated -> List(updated.request, updated.nextTick)

  def nextTick: Delayed[Command] = Delayed(Command.Tick(ref), Delay.ActionTimeout)

object Timer:
  def ref[A](a: A)(using encoder: Encoder[A]): Int =
    // one option here would be to use a.hashCode,
    // but that's probably going to return different values for different application execution
    // which would make it difficult to test. hashCode on a string seems to be consistent
    encoder(a).printWith(Printer.noSpaces.copy(dropNullValues = true, sortKeys = true)).hashCode
