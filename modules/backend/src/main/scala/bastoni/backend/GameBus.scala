package bastoni.backend

import bastoni.domain.model.*
import bastoni.domain.model.Command.*
import bastoni.domain.model.Event.*
import bastoni.domain.view.{FromPlayer, ToPlayer}
import cats.effect.Sync

class GameBus[F[_]](messageBus: MessageBus[F], seeds: fs2.Stream[F, Int]):

  def subscribe(me: Player, roomId: RoomId): fs2.Stream[F, ToPlayer] =
    messageBus
      .subscribe
      .collect { case Message(`roomId`, event: (Event | ActionRequest)) => event }
      .map {
        case PlayerJoined(player, room)                 => ToPlayer.PlayerJoined(player, room)
        case PlayerLeft(player, room)                   => ToPlayer.PlayerLeft(player, room)
        case GameStarted(gameType)                      => ToPlayer.GameStarted(gameType)
        case DeckShuffled(_)                            => ToPlayer.DeckShuffled
        case CardDealt(player, card) if player == me.id => ToPlayer.CardDealt(player, Some(card))
        case CardDealt(player, _)                       => ToPlayer.CardDealt(player, None)
        case TrumpRevealed(trump)                       => ToPlayer.TrumpRevealed(trump)
        case CardPlayed(player, card)                   => ToPlayer.CardPlayed(player, card)
        case TrickCompleted(player)                     => ToPlayer.TrickCompleted(player)
        case PointsCount(playerIds, points)             => ToPlayer.PointsCount(playerIds, points)
        case TotalPointsCount(playerIds, points)        => ToPlayer.TotalPointsCount(playerIds, points)
        case MatchCompleted(winners)                    => ToPlayer.MatchCompleted(winners)
        case MatchDraw                                  => ToPlayer.MatchDraw
        case MatchAborted                               => ToPlayer.MatchAborted
        case GameCompleted(winners)                     => ToPlayer.GameCompleted(winners)
        case GameAborted                                => ToPlayer.GameAborted
        case ActionRequest(player, action)              => ToPlayer.ActionRequest(player, action)
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
      .map(Message(roomId, _))
      .through(messageBus.publish)


object GameBus:
  def apply[F[_]: Sync](bus: MessageBus[F]): GameBus[F] =
    new GameBus(bus, fs2.Stream.repeatEval(Sync[F].delay(scala.util.Random.nextInt())))
