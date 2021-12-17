package bastoni.domain.logic

import bastoni.domain.repos.*
import cats.effect.{Async, Resource}

object Services:

  def apply[F[_]: Async](
    messageBus: MessageBus[F],
    snapshotBus: SnapshotBus[F],
    gameRepo: GameRepo[F],
    messageRepo: MessageRepo[F],
    tableRepo: TableRepo[F],
    roomRepo: RoomRepo[F]
  ): Resource[F, (GamePublisher[F], GameSubscriber[F], fs2.Stream[F, Unit])] =
    for {
      gameService         <- GameService.runner(messageBus, gameRepo, messageRepo)
      lobby               <- Lobby.runner(messageBus, roomRepo)
      gameSnapshotService <- GameSnapshotService.runner(messageBus, snapshotBus, tableRepo)

      pub = GameSnapshotService.publisher(messageBus)
      sub = GameSnapshotService.subscriber(snapshotBus)

      servicesRunner =
        messageBus.run
          .concurrently(snapshotBus.run)
          .concurrently(gameService)
          .concurrently(lobby)
          .concurrently(gameSnapshotService)
    } yield (pub, sub, servicesRunner)

  def inMemory[F[_]: Async]: Resource[F, (GamePublisher[F], GameSubscriber[F], fs2.Stream[F, Unit])] =
    for {
      messageBus  <- Resource.eval(MessageBus.inMemory)
      snapshotBus <- Resource.eval(SnapshotBus.inMemory)
      gameRepo    <- Resource.eval(GameRepo.inMemory)
      messageRepo <- Resource.eval(MessageRepo.inMemory)
      tableRepo   <- Resource.eval(TableRepo.inMemory)
      roomRepo    <- Resource.eval(RoomRepo.inMemory)
      services    <- Services(messageBus, snapshotBus, gameRepo, messageRepo, tableRepo, roomRepo)
    } yield services
