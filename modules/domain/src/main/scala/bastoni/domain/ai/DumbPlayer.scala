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

object DumbPlayer:
  def apply[F[_]: Sync: Temporal](me: User, roomId: RoomId, subscriber: GameSubscriber[F], publisher: GamePublisher[F], pause: FiniteDuration = 0.millis): fs2.Stream[F, Unit] =
    val actions: fs2.Stream[F, FromPlayer] =
      fs2.Stream(Connect, JoinRoom) ++ subscriber
        .subscribe(me, roomId)
        .zipWithScan1(Option.empty[RoomPlayerView]) {
          case (_, ToPlayer.Snapshot(room)) => Some(room)
          case (room, ToPlayer.Request(request)) => room.map(_.withRequest(request))
          case (room, ToPlayer.GameEvent(event)) => room.map(_.update(event))
        }
        .collect {
          case (ToPlayer.Request(Command.Act(playerId, action, _)), Some(room)) if me.is(playerId) =>
            (room, action, room.seatFor(me).getOrElse(throw new IllegalStateException("I am not sat at this table")))
        }
        .collect { case (room, action, seat) => act(room, action, seat) }
        .evalMap(command => Sync[F].pure(command).delayBy(pause))

    actions
      // .evalTap { act => Sync[F].delay(println(s"${me.name} will ${act.getClass.getSimpleName.filter(_ != '$')}")) }
      .through(publisher.publish(me, roomId))

  def act(room: RoomPlayerView, action: Action, seat: TakenSeat[CardPlayerView]): FromPlayer = {
    val hand: List[VisibleCard] = seat.hand.flatMap(_.card.toOption)
    
    action match {
      case Action.PlayCard =>
        PlayCard(hand.pickFirst)

      case Action.PlayCardOf(suit) =>
        PlayCard(hand.find(_.suit == suit).orElse(hand.headOption).toList.pickFirst)

      case Action.TakeCards =>
        val cardToPlay: VisibleCard = hand.pickFirst
        val board: List[VisibleCard] = room.board.flatMap { case (_, c) => c.card.toOption }
        val takes = ScopaGame.takeCombinations(board, cardToPlay).next().toList
        TakeCards(cardToPlay, takes)

      case Action.ShuffleDeck => ShuffleDeck

      case Action.Confirm => Ok
    }
  }

  extension (cards: List[VisibleCard])
    def pickFirst: VisibleCard = cards.headOption.getOrElse(throw new IllegalStateException("No cards in hand"))
