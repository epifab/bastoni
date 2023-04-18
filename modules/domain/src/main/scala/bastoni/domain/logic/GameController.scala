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
  def publish1(me: User, roomId: RoomId)(input: FromPlayer): F[Unit]

trait GameController[F[_]] extends GameSubscriber[F] with GamePublisher[F]

object GameController:

  def apply[F[_]: Sync](messageBus: MessageBus[F]): GameController[F] =
    val pub = publisher(messageBus)
    val sub = subscriber(messageBus)
    new GameController[F]:
      override def publish(me: User, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
        pub.publish(me, roomId)(input)

      override def publish1(me: User, roomId: RoomId)(input: FromPlayer): F[Unit] =
        pub.publish1(me, roomId)(input)

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
              case PlayerConnected(user, room) if user.is(me) => ToPlayer.Connected(room.toPlayerView(me.id))
            }
        )

  def publisher[F[_]: Sync](messageBus: MessagePublisher[F]): GamePublisher[F] =
    publisher(
      messageBus,
      Sync[F].delay(Random.nextInt()),
      Sync[F].delay(MessageId.newId)
    )

  def publisher[F[_]: Monad](
      messageBus: MessagePublisher[F],
      seed: F[Int],
      messageId: F[MessageId]
  ): GamePublisher[F] = new GamePublisher[F]:
    override def publish(me: User, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
      input.evalMap(publish1(me, roomId))

    override def publish1(me: User, roomId: RoomId)(input: FromPlayer): F[Unit] =
      seed
        .map(seed => buildCommand(me)(input -> seed))
        .flatMap {
          case Some(command) =>
            messageId.flatMap(id => messageBus.publish1(Message(id, roomId, command)))
          case None =>
            Monad[F].unit
        }

  def buildCommand(me: User)(eventAndSeed: (FromPlayer, Int)): Option[Command] =
    Some(eventAndSeed).collect {
      case (FromPlayer.Connect, _)                => Connect(me)
      case (FromPlayer.JoinRoom, seed)            => JoinRoom(me, seed)
      case (FromPlayer.LeaveRoom, _)              => LeaveRoom(me)
      case (FromPlayer.StartMatch(gameType), _)   => StartMatch(me.id, gameType)
      case (FromPlayer.ShuffleDeck, seed)         => ShuffleDeck(seed)
      case (FromPlayer.Ok, _)                     => Ok(me.id)
      case (FromPlayer.PlayCard(card), _)         => PlayCard(me.id, card)
      case (FromPlayer.TakeCards(card, taken), _) => TakeCards(me.id, card, taken)
    }
end GameController
