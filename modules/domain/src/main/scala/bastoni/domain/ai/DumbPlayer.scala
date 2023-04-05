package bastoni.domain.ai

import bastoni.domain.logic.{GameController, GamePublisher, GameSubscriber}
import bastoni.domain.logic.scopa.ScopaGame
import bastoni.domain.model.*
import bastoni.domain.model.PlayerState.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import bastoni.domain.view.FromPlayer.*
import cats.effect.{Sync, Temporal}
import cats.effect.syntax.temporal.*

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.chaining.*

object DumbPlayer extends ActStrategy:
  def apply[F[_]: Sync: Temporal](
      controller: GameController[F],
      pause: FiniteDuration = 0.millis
  ): VirtualPlayer[F] =
    VirtualPlayer(controller, DumbPlayer, pause)

  def act(context: ActContext, action: Action): FromPlayer =
    act(context.room, context.mySeat, action)

  def act(room: Room[CardPlayerView], seat: TakenSeat[CardPlayerView], action: Action): FromPlayer =
    val hand: List[VisibleCard] = seat.hand.flatMap(_.card.toOption)

    action match
      case Action.PlayCard(PlayContext.Briscola(_)) =>
        PlayCard(hand.pickFirst)

      case Action.PlayCard(PlayContext.Tressette(Some(suit))) =>
        PlayCard(hand.find(_.suit == suit).orElse(hand.headOption).toList.pickFirst)

      case Action.PlayCard(PlayContext.Tressette(None)) =>
        PlayCard(hand.pickFirst)

      case Action.PlayCard(PlayContext.Scopa) =>
        val cardToPlay: VisibleCard  = hand.pickFirst
        val board: List[VisibleCard] = room.board.flatMap(_.card.toOption)
        val takes                    = ScopaGame.takeCombinations(board, cardToPlay).next().toList
        TakeCards(cardToPlay, takes)

      case Action.ShuffleDeck => ShuffleDeck

      case Action.Confirm => Ok

  extension (cards: List[VisibleCard])
    def pickFirst: VisibleCard = cards.headOption.getOrElse(throw new IllegalStateException("No cards in hand"))
end DumbPlayer
