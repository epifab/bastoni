package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.repos.TableRepo
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.Monad
import cats.effect.syntax.all.*
import cats.effect.{Resource, Sync}
import cats.syntax.all.*

import scala.util.Random

trait GameSubscriber[F[_]]:
  def subscribe(me: Player, roomId: RoomId): fs2.Stream[F, ToPlayer]

trait GamePublisher[F[_]]:
  def publish(me: Player, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit]

object GameSnapshotService:

  def runner[F[_]: Monad](messageBus: MessageBus[F], snapshotBus: SnapshotBus[F], tableRepo: TableRepo[F]): ServiceRunner[F] =
    messageBus.subscribeAwait.map { subscription =>
      subscription
        .evalMap {
          case Message(_, roomId, data) =>
            for {
              existingTable <- tableRepo.get(roomId)
              updatedTable = data match {
                case event: ServerEvent => existingTable.map(_.update(event)).orElse(Table(event))
                case command: Command => existingTable
              }
              _ <- updatedTable.fold(tableRepo.remove(roomId))(tableRepo.set(roomId, _))
              hasUpdate = existingTable != updatedTable
            } yield Option.when(hasUpdate)(roomId -> updatedTable)
        }
        .collect { case Some(update) => update }
        .through(snapshotBus.publish)
    }

  def subscriber[F[_]](snapshotBus: SnapshotBus[F]): GameSubscriber[F] =
    new GameSubscriber[F] {
      override def subscribe(me: Player, roomId: RoomId): fs2.Stream[F, ToPlayer] =
        snapshotBus.subscribe.collect { case (`roomId`, Some(table)) => ToPlayer.Snapshot(table.toPlayerView(me)) }
    }

  def publisher[F[_]](
    messageBus: MessageBus[F],
    seeds: fs2.Stream[F, Int],
    messageIds: fs2.Stream[F, MessageId]
  ): GamePublisher[F] = new GamePublisher[F] {
    override def publish(me: Player, roomId: RoomId)(input: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
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

  private def buildCommand(me: Player)(eventAndSeed: (FromPlayer, Int)): Command =
    eventAndSeed match
      case (FromPlayer.JoinRoom, _)               => JoinRoom(me)
      case (FromPlayer.LeaveRoom, _)              => LeaveRoom(me)
      case (FromPlayer.ActivateRoom(gameType), _) => ActivateRoom(me, gameType)
      case (FromPlayer.ShuffleDeck, seed)         => ShuffleDeck(seed)
      case (FromPlayer.PlayCard(card), _)         => PlayCard(me.id, card)
