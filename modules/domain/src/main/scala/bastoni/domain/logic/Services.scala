package bastoni.domain.logic

import bastoni.domain.repos.*
import cats.effect.{Concurrent, Resource, Async}

object Services:

  def apply[F[_]: Async](
    messageBus: MessageBus[F],
    snapshotBus: SnapshotBus[F],
    gameRepo: GameRepo[F],
    messageRepo: MessageRepo[F],
    tableRepo: TableRepo[F],
    roomRepo: RoomRepo[F]
  ): Resource[F, fs2.Stream[F, Unit]] =
    for {
      gameService         <- GameService.runner(messageBus, gameRepo, messageRepo)
      lobby               <- Lobby.runner(messageBus, roomRepo)
      gameSnapshotService <- GameSnapshotService.runner(messageBus, snapshotBus, tableRepo)
    } yield {
      messageBus.run
        .concurrently(snapshotBus.run)
        .concurrently(gameService)
        .concurrently(lobby)
        .concurrently(gameSnapshotService)
    }

  def inMemory[F[_]: Async] =
    for {
      messageBus  <- Resource.eval(MessageBus.inMemory)
      snapshotBus <- Resource.eval(SnapshotBus.inMemory)
      gameRepo    <- Resource.eval(GameRepo.inMemory)
      messageRepo <- Resource.eval(MessageRepo.inMemory)
      tableRepo   <- Resource.eval(TableRepo.inMemory)
      roomRepo    <- Resource.eval(RoomRepo.inMemory)
      services    <- Services(messageBus, snapshotBus, gameRepo, messageRepo, tableRepo, roomRepo)
    } yield services
