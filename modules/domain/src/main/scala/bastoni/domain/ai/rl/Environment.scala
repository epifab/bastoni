package bastoni.domain.ai.rl

import cats.data.RWS

type Reward = Double

object Reward:
  val zero: Reward = 0.0

  given descending: Ordering[Reward] with
    def compare(a: Reward, b: Reward): Int =
      if (a == b) 0 else if (a < b) -1 else 1


trait Environment[State, Action]:

  def step(currentState: State, actionTaken: Action): (State, Reward)

  def isTerminal(state: State): Boolean
