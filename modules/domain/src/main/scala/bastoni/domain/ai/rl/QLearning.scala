package bastoni.domain.ai.rl

import scala.util.Random

trait ActionableState[State, Action]:
  def validActions(state: State): Set[Action]

case class QLearning[State, Action](
  stepSize: Double,
  discountRate: Double,
  explorationRate: Double,
  Q: Map[State, Map[Action, Reward]]
):
  def tableFor(state: State)(using actionableState: ActionableState[State, Action]): Map[Action, Reward] =
    Q.getOrElse(
      state,
      actionableState.validActions(state).map(_ -> Reward.zero).toMap
    )

object QLearning:
  private def epsilonGreedy[Action](actionValues: Map[Action, Reward], explorationRate: Double): (Action, Reward) = {
    if (Random.nextGaussian() < explorationRate) {
      Random.shuffle(actionValues.toList).head
    } else {
      val sorted = actionValues.toList.sortBy(_._2)(using Reward.descending)
      val maxValue = sorted.head._2
      Random.shuffle(sorted.takeWhile(_._2 == maxValue)).head
    }
  }

  given [State, Action](using ActionableState[State, Action]): AgentBehaviour[QLearning[State, Action], State, Action] with
    override def chooseAction(
      agentData: QLearning[State, Action],
      state: State
    ): (Action, ActionResult[State] => QLearning[State, Action]) = {
      val table: Map[Action, Reward] = agentData.tableFor(state)
      val (chosenAction: Action, currentReward: Reward) = epsilonGreedy(table, agentData.explorationRate)
      chosenAction -> { (actionResult: ActionResult[State]) =>
        val nextRewards: Set[Reward] = actionResult
          .possibleStates
          .map(nextState => agentData.tableFor(nextState).values.max)

        val nextReward = nextRewards.sum / nextRewards.size

        val updatedActionReward = currentReward + agentData.stepSize * (actionResult.reward + agentData.discountRate * nextReward - currentReward)
        val updatedTable = table + (chosenAction -> updatedActionReward)
        val updatedQ = agentData.Q + (state -> updatedTable)
        agentData.copy(Q = updatedQ)
      }
    }
