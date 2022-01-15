package bastoni.domain.logic

import bastoni.domain.model.*
import cats.Applicative

import scala.annotation.tailrec

trait GameLogic[State]:

  protected val uneventful: List[StateMachineOutput] = Nil

  val gameType: GameType
  def initialState(users: List[User]): State
  def isFinal(state: State): Boolean

  val playStep: (State, StateMachineInput) => (State, List[StateMachineOutput])

  @tailrec
  private def playSteps(state: State, input: List[StateMachineInput], previousEvents: List[StateMachineOutput] = Nil): (State, List[StateMachineOutput]) =
    input match {
      case Nil => state -> previousEvents
      case head :: tail =>
        val (intermediateState, events) = playStep(state, head)
        playSteps(intermediateState, tail, previousEvents ++ events)
    }
  
  def playSteps(state: State, input: StateMachineInput, otherInput: StateMachineInput*): (State, List[StateMachineOutput]) =
    playSteps(state, input :: otherInput.toList, previousEvents = uneventful)
  
  def playStream[F[_]](users: List[User])(input: fs2.Stream[F, StateMachineInput]): fs2.Stream[F, StateMachineOutput] =
    input
      .scan[(State, List[StateMachineOutput])](initialState(users) -> uneventful) {
        case ((state, _), message) => playStep(state, message)
      }
      .takeThrough { case (state, _) => !isFinal(state) }
      .flatMap { case (state, output) => fs2.Stream.iterable(output) }
