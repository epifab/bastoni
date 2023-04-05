package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.{Resource, Sync}
import cats.effect.syntax.all.*
import cats.syntax.all.*
import cats.Monad

import scala.util.Random

trait GameSubscriber[F[_]]:
  def subscribe(me: User, roomId: RoomId): fs2.Stream[F, ToPlayer]

trait GamePublisher[F[_]]:
  def publish(me: User, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit]

type GameController[F[_]] = GameSubscriber[F] with GamePublisher[F]

object GameController:

  def apply[F[_]: Sync](messageBus: MessageBus[F]): GameController[F] =
    val pub = publisher(messageBus)
    val sub = subscriber(messageBus)
    new GameSubscriber[F] with GamePublisher[F]:
      override def publish(me: User, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
        pub.publish(me, roomId)(input)

      override def subscribe(me: User, roomId: RoomId): fs2.Stream[F, ToPlayer] =
        sub.subscribe(me, roomId)

  def subscriber[F[_]: Sync](messageBus: MessageBus[F]): GameSubscriber[F] =
    (me: User, roomId: RoomId) =>
      fs2.Stream
        .resource(messageBus.subscribe)
        .flatMap(stream =>
          stream
            .collect { case Message(_, `roomId`, event: (Event | Command)) => event }
            .collect {
              case command: Command.Act => ToPlayer.Request(command)
              case event: PublicEvent   => ToPlayer.GameEvent(event)
              case ServerOnlyEvent.CardsDealt(playerId, cards) =>
                ToPlayer.GameEvent(
                  PlayerOnlyEvent.CardsDealt(
                    playerId,
                    cards.map(_.toPlayerView(me.id, Some(playerId)))
                  )
                )
              case ServerOnlyEvent.DeckShuffled(deck) => ToPlayer.GameEvent(PlayerOnlyEvent.DeckShuffled(deck.size))
              case PlayerConnected(room)              => ToPlayer.Snapshot(room.toPlayerView(me.id))
            }
        )

  def publisher[F[_]: Sync](messageBus: MessagePublisher[F]): GamePublisher[F] =
    publisher(
      messageBus,
      fs2.Stream.repeatEval(Sync[F].delay(Random.nextInt())),
      fs2.Stream.repeatEval(Sync[F].delay(MessageId.newId))
    )

  def publisher[F[_]](
      messageBus: MessagePublisher[F],
      seeds: fs2.Stream[F, Int],
      messageIds: fs2.Stream[F, MessageId]
  ): GamePublisher[F] = new GamePublisher[F]:
    override def publish(me: User, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
      input
        .zip(seeds)
        .map(buildCommand(me))
        .zip(messageIds)
        .map { case (message, id) => Message(id, roomId, message) }
        .through(messageBus.publish)

  def buildCommand(me: User)(eventAndSeed: (FromPlayer, Int)): Command =
    eventAndSeed match
      case (FromPlayer.Connect, _)                => Connect
      case (FromPlayer.JoinRoom, seed)            => JoinRoom(me, seed)
      case (FromPlayer.LeaveRoom, _)              => LeaveRoom(me)
      case (FromPlayer.StartMatch(gameType), _)   => StartMatch(me.id, gameType)
      case (FromPlayer.ShuffleDeck, seed)         => ShuffleDeck(seed)
      case (FromPlayer.Ok, _)                     => Ok(me.id)
      case (FromPlayer.PlayCard(card), _)         => PlayCard(me.id, card)
      case (FromPlayer.TakeCards(card, taken), _) => TakeCards(me.id, card, taken)
end GameController
