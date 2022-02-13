package bastoni.domain.ai
import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.domain.view.FromPlayer.{Ok, PlayCard, ShuffleDeck}

object GreedyPlayer extends ActStrategy:
  override def act(context: ActContext, action: Action): FromPlayer = {
    action match {
      case Action.PlayCard(PlayContext.Briscola(trump)) => ???

      case Action.PlayCard(PlayContext.Tressette(_)) => ???

      case Action.PlayCard(PlayContext.Scopa) => ???

      case Action.ShuffleDeck => ShuffleDeck

      case Action.Confirm => Ok
    }
  }