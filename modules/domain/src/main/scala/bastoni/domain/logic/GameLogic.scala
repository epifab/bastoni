package bastoni.domain.logic

import bastoni.domain.model.{GameType, MatchPlayer, User}
import bastoni.domain.model.Command.{Act, Continue}
import bastoni.domain.model.Delay.syntax.afterGameOver
import cats.Applicative

import scala.annotation.tailrec

trait GameLogic[State]:
  def gameType: GameType
  def play: (State, StateMachineInput) => (State, List[StateMachineOutput])

  def initialState(players: List[MatchPlayer]): State
  def isFinal(state: State): Boolean

  def playStream[F[_]](users: List[User]): fs2.Stream[F, StateMachineInput] => fs2.Stream[F, StateMachineOutput] =
    playStream(initialState(users.map(MatchPlayer(_, 0))))

  def playStream[F[_]](initialState: State)(input: fs2.Stream[F, StateMachineInput]): fs2.Stream[F, StateMachineOutput] =
    input
      .scan[(State, List[StateMachineOutput])](initialState -> Nil) { case ((state, _), message) => play(state, message) }
      .takeThrough { case (state, _) => !isFinal(state) }
      .flatMap { case (_, output) => fs2.Stream.iterable(output) }
