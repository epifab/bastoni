package bastoni.domain.logic

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.Sync

class GameBus[F[_]](messageBus: MessageBus[F], seeds: fs2.Stream[F, Int], messageIds: fs2.Stream[F, MessageId]):

  def subscribe(me: Player, roomId: RoomId): fs2.Stream[F, ToPlayer] =
    messageBus
      .subscribe
      .collect { case Message(_, `roomId`, event: (Event | ActionRequest)) => event }
      .map {
        case PlayerJoined(player, room)                       => ToPlayer.PlayerJoined(player, room)
        case PlayerLeft(player, room)                         => ToPlayer.PlayerLeft(player, room)
        case GameStarted(gameType)                            => ToPlayer.GameStarted(gameType)
        case DeckShuffled(cards)                              => ToPlayer.DeckShuffled(cards.size)
        case CardDealt(player, card, _) if player == me.id    => ToPlayer.CardDealt(player, Some(card))
        case CardDealt(player, card, Face.Up)                 => ToPlayer.CardDealt(player, Some(card))
        case CardDealt(player, _, _)                          => ToPlayer.CardDealt(player, None)
        case TrumpRevealed(trump)                             => ToPlayer.TrumpRevealed(trump)
        case CardPlayed(player, card)                         => ToPlayer.CardPlayed(player, card)
        case TrickCompleted(player)                           => ToPlayer.TrickCompleted(player)
        case MatchCompleted(winners, matchPoints, gamePoints) => ToPlayer.MatchCompleted(winners, matchPoints, gamePoints)
        case MatchAborted                                     => ToPlayer.MatchAborted
        case GameCompleted(winners)                           => ToPlayer.GameCompleted(winners)
        case GameAborted                                      => ToPlayer.GameAborted
        case ActionRequest(player, action)                    => ToPlayer.ActionRequest(player, action)
      }

  private def toModel(me: Player)(eventAndSeed: (FromPlayer, Int)): Command =
    eventAndSeed match
      case (FromPlayer.JoinRoom, _)               => JoinRoom(me)
      case (FromPlayer.LeaveRoom, _)              => LeaveRoom(me)
      case (FromPlayer.ActivateRoom(gameType), _) => ActivateRoom(me, gameType)
      case (FromPlayer.ShuffleDeck, seed)         => ShuffleDeck(seed)
      case (FromPlayer.PlayCard(card), _)         => PlayCard(me.id, card)

  def publish(me: Player, roomId: RoomId, events: fs2.Stream[F, FromPlayer]): fs2.Stream[F, Unit] =
    events
      .zip(seeds)
      .map(toModel(me))
      .zip(messageIds)
      .map { case (message, id) => Message(id, roomId, message) }
      .through(messageBus.publish)


object GameBus:
  def apply[F[_]: Sync](bus: MessageBus[F]): GameBus[F] =
    new GameBus(
      bus,
      fs2.Stream.repeatEval(Sync[F].delay(scala.util.Random.nextInt())),
      fs2.Stream.repeatEval(Sync[F].delay(MessageId.newId))
    )
