package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.Monad
import cats.effect.syntax.all.*
import cats.effect.{Resource, Sync}
import cats.syntax.all.*

import scala.util.Random

trait GameSubscriber[F[_]]:
  def subscribe(me: User, roomId: RoomId): fs2.Stream[F, ToPlayer]

trait GamePublisher[F[_]]:
  def publish(me: User, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit]

object GamePubSub:

  def subscriber[F[_]](messageBus: MessageBus[F]): GameSubscriber[F] =
    new GameSubscriber[F] {
      override def subscribe(me: User, roomId: RoomId): fs2.Stream[F, ToPlayer] =
        messageBus
          .subscribe
          .collect { case Message(_, `roomId`, event: Event) => event }
          .collect {
            case event: PublicEvent => ToPlayer.GameEvent(event)
            case CardsDealtServerView(playerId, cards) => ToPlayer.GameEvent(CardsDealtPlayerView(playerId, cards.map(_.toPlayerView(me.id, Some(playerId)))))
            case DeckShuffledServerView(deck) => ToPlayer.GameEvent(DeckShuffledPlayerView(deck.size))
            case Snapshot(table) => ToPlayer.Snapshot(table.toPlayerView(me))
          }
    }

  def publisher[F[_]](
    messageBus: MessageBus[F],
    seeds: fs2.Stream[F, Int],
    messageIds: fs2.Stream[F, MessageId]
  ): GamePublisher[F] = new GamePublisher[F] {
    override def publish(me: User, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
      input
        .zip(seeds)
        .map(buildCommand(me))
        .zip(messageIds)
        .map { case (message, id) => Message(id, roomId, message) }
        .through(messageBus.publish)
  }

  def publisher[F[_]: Sync](messageBus: MessageBus[F]): GamePublisher[F] =
    publisher(
      messageBus,
      fs2.Stream.repeatEval(Sync[F].delay(Random.nextInt())),
      fs2.Stream.repeatEval(Sync[F].delay(MessageId.newId))
    )

  private def buildCommand(me: User)(eventAndSeed: (FromPlayer, Int)): Command =
    eventAndSeed match
      case (FromPlayer.Connect, _)                => Connect
      case (FromPlayer.JoinTable, seed)           => JoinTable(me, seed)
      case (FromPlayer.LeaveTable, _)             => LeaveTable(me)
      case (FromPlayer.StartGame(gameType), _)    => StartGame(me.id, gameType)
      case (FromPlayer.ShuffleDeck, seed)         => ShuffleDeck(seed)
      case (FromPlayer.PlayCard(card), _)         => PlayCard(me.id, card)
      case (FromPlayer.TakeCards(card, taken), _) => TakeCards(me.id, card, taken)
