package bastoni.domain.logic

import bastoni.domain.repos.*
import cats.effect.{Async, Resource}
import org.typelevel.log4cats.Logger

object Services:

  def apply[F[_]: Async: Logger](
      instances: Int,
      messageQueue: MessageQueue[F],
      messageBus: MessageBus[F],
      gameRepo: GameRepo[F],
      messageRepo: MessageRepo[F]
  ): (GameController[F], fs2.Stream[F, Unit]) =
    val runners =
      (1 to instances)
        .map(n =>
          GameService.runner(
            s"game-service-$n",
            messageQueue,
            messageBus,
            gameRepo,
            messageRepo
          )
        )
        .reduce(_ concurrently _)
    val gameController = GameController(messageBus)
    val servicesRunner = messageBus.run
      .concurrently(messageQueue.run)
      .concurrently(runners)
    (gameController, servicesRunner)

  def inMemory[F[_]: Async: Logger]: Resource[F, (GameController[F], fs2.Stream[F, Unit])] =
    for
      messageBus   <- Resource.eval(MessageBus.inMemory)
      messageQueue <- MessageQueue.inMemory(messageBus)
      gameRepo     <- Resource.eval(GameRepo.inMemory)
      messageRepo  <- Resource.eval(MessageRepo.inMemory)
    yield Services(1, messageQueue, messageBus, gameRepo, messageRepo)
end Services
