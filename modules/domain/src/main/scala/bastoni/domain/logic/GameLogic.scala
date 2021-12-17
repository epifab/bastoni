package bastoni.domain.logic

import bastoni.domain.model.*
import cats.Applicative

trait GameLogic[State]:

  protected val uneventful: List[StateMachineOutput] = Nil

  val playStep: (State, StateMachineInput) => (State, List[StateMachineOutput])
  def initialState(users: List[User]): State
  def isFinal(state: State): Boolean

  def playStream[F[_]](users: List[User])(input: fs2.Stream[F, StateMachineInput]): fs2.Stream[F, StateMachineOutput] =
    input
      .scan[(State, List[StateMachineOutput])](initialState(users) -> uneventful) {
        case ((state, _), message) => playStep(state, message)
      }
      .takeThrough { case (state, _) => !isFinal(state) }
      .flatMap { case (state, output) => fs2.Stream.iterable(output) }
