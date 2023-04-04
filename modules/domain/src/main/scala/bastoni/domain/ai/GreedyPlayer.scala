package bastoni.domain.ai

import bastoni.domain.logic.briscola.{BriscolaGame, BriscolaGameScore, BriscolaGameScoreCalculator}
import bastoni.domain.model.*
import bastoni.domain.view.FromPlayer
import bastoni.domain.view.FromPlayer.{Ok, PlayCard, ShuffleDeck}

object GreedyPlayer extends ActStrategy:
  override def act(context: ActContext, action: Action): FromPlayer =
    action match
      case Action.PlayCard(PlayContext.Briscola(trump)) =>
        val myHand: List[VisibleCard] = context.mySeat.hand.flatMap(_.card.toOption)
        val board: List[VisibleCard]  = context.room.board.flatMap(_.card.toOption)

        // winning and losing cards in hand sorted by their value
        val (win, lose) = myHand
          .sorted(using BriscolaGame.order)
          .partition(myCard => BriscolaGame.bestCard(trump, board :+ myCard) == myCard)

        val pointsToWin: Int = board.map(BriscolaGameScoreCalculator.pointsFor).sum
        val pointsToLose     = lose.headOption.map(BriscolaGameScoreCalculator.pointsFor).getOrElse(0)

        val toPlay: VisibleCard =
          (if (context.room.board.isEmpty) win.find(_.suit != trump) else win.findLast(_.suit != trump))
            .orElse { if (lose.isEmpty || pointsToWin > 4 || pointsToLose > 4) win.headOption else None }
            .orElse { lose.headOption }
            .getOrElse(throw new IllegalStateException("No cards in hand"))

        PlayCard(toPlay)

      case Action.PlayCard(PlayContext.Tressette(_)) =>
        // todo: implement me
        DumbPlayer.act(context, action)

      case Action.PlayCard(PlayContext.Scopa) =>
        // todo: implement me
        DumbPlayer.act(context, action)

      case Action.ShuffleDeck => ShuffleDeck

      case Action.Confirm => Ok
end GreedyPlayer
