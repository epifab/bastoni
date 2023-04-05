package bastoni.domain.logic

import bastoni.domain.repos.*
import cats.effect.{Async, Resource}

object Services:

  def apply[F[_]: Async](
      messageQueue: MessageQueue[F],
      messageBus: MessageBus[F],
      gameRepo: GameRepo[F],
      messageRepo: MessageRepo[F]
  ): Resource[F, (GamePublisher[F], GameSubscriber[F], fs2.Stream[F, Unit])] =
    for
      gameService1 <- GameService.runner("vegas01", messageQueue, messageBus, gameRepo, messageRepo)
      gameService2 <- GameService.runner("vegas02", messageQueue, messageBus, gameRepo, messageRepo)
      pub = GamePubSub.publisher(messageBus)
      sub = GamePubSub.subscriber(messageBus)

      servicesRunner = messageBus.run
        .concurrently(messageQueue.run)
        .concurrently(gameService1)
        .concurrently(gameService2)
    yield (pub, sub, servicesRunner)

  def inMemory[F[_]: Async]: Resource[F, (GamePublisher[F], GameSubscriber[F], fs2.Stream[F, Unit])] =
    for
      messageBus   <- Resource.eval(MessageBus.inMemory)
      messageQueue <- MessageQueue.inMemory(messageBus)
      gameRepo     <- Resource.eval(GameRepo.inMemory)
      messageRepo  <- Resource.eval(MessageRepo.inMemory)
      services     <- Services(messageQueue, messageBus, gameRepo, messageRepo)
    yield services
