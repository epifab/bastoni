package bastoni.domain.ai.rl

case class ActionResult[State](reward: Reward, possibleStates: Set[State])

trait AgentBehaviour[AgentData, State, Action]:
  def chooseAction(agentData: AgentData, state: State): (Action, ActionResult[State] => AgentData)
