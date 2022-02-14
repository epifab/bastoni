package bastoni.domain.ai

import bastoni.domain.logic.scopa.ScopaGame
import bastoni.domain.logic.{GamePublisher, GameSubscriber}
import bastoni.domain.model.*
import bastoni.domain.model.PlayerState.*
import bastoni.domain.view.FromPlayer.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.syntax.temporal.*
import cats.effect.{Sync, Temporal}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.chaining.*

object DumbPlayer extends ActStrategy:
  def apply[F[_]: Sync: Temporal](publisher: GamePublisher[F], subscriber: GameSubscriber[F], pause: FiniteDuration = 0.millis): VirtualPlayer[F] =
    VirtualPlayer(publisher, subscriber, DumbPlayer, pause)

  def act(context: ActContext, action: Action): FromPlayer =
    act(context.room, context.mySeat, action)

  def act(room: Room[CardPlayerView], seat: TakenSeat[CardPlayerView], action: Action): FromPlayer = {
    val hand: List[VisibleCard] = seat.hand.flatMap(_.card.toOption)
    
    action match {
      case Action.PlayCard(PlayContext.Briscola(_)) =>
        PlayCard(hand.pickFirst)

      case Action.PlayCard(PlayContext.Tressette(Some(suit))) =>
        PlayCard(hand.find(_.suit == suit).orElse(hand.headOption).toList.pickFirst)

      case Action.PlayCard(PlayContext.Tressette(None)) =>
        PlayCard(hand.pickFirst)

      case Action.PlayCard(PlayContext.Scopa) =>
        val cardToPlay: VisibleCard = hand.pickFirst
        val board: List[VisibleCard] = room.board.flatMap(_.card.toOption)
        val takes = ScopaGame.takeCombinations(board, cardToPlay).next().toList
        TakeCards(cardToPlay, takes)

      case Action.ShuffleDeck => ShuffleDeck

      case Action.Confirm => Ok
    }
  }

  extension (cards: List[VisibleCard])
    def pickFirst: VisibleCard = cards.headOption.getOrElse(throw new IllegalStateException("No cards in hand"))
