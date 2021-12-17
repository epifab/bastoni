package bastoni.domain.logic

import bastoni.domain.repos.*
import cats.effect.{Async, Resource}

object Services:

  def apply[F[_]: Async](
    messageBus: MessageBus[F],
    gameRepo: GameRepo[F],
    messageRepo: MessageRepo[F]
  ): Resource[F, (GamePublisher[F], GameSubscriber[F], fs2.Stream[F, Unit])] =
    for {
      gameService <- GameService.runner(messageBus, gameRepo, messageRepo)
      pub = GamePubSub.publisher(messageBus)
      sub = GamePubSub.subscriber(messageBus)

      servicesRunner = messageBus.run.concurrently(gameService)
    } yield (pub, sub, servicesRunner)

  def inMemory[F[_]: Async]: Resource[F, (GamePublisher[F], GameSubscriber[F], fs2.Stream[F, Unit])] =
    for {
      messageBus  <- Resource.eval(MessageBus.inMemory)
      gameRepo    <- Resource.eval(GameRepo.inMemory)
      messageRepo <- Resource.eval(MessageRepo.inMemory)
      services    <- Services(messageBus, gameRepo, messageRepo)
    } yield services
