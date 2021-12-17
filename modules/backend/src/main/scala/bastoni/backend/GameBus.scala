package bastoni.backend

import bastoni.domain.view.FromPlayer
import bastoni.domain.{model, view}

class GameBus[F[_]](messageBus: MessageBus[F], seeds: fs2.Stream[F, Int]):

  def subscribe(me: model.Player, roomId: model.RoomId): fs2.Stream[F, view.ToPlayer] =
    messageBus
      .subscribe
      .collect { case model.Message(`roomId`, event: (model.Event | model.ActionRequest)) => event }
      .map {
        case model.PlayerJoined(player, room)                 => view.PlayerJoined(player, room)
        case model.PlayerLeft(player, room)                   => view.PlayerLeft(player, room)
        case model.DeckShuffled(_)                            => view.DeckShuffled
        case model.CardDealt(player, card) if player == me.id => view.CardDealt(player, Some(card))
        case model.CardDealt(player, _)                       => view.CardDealt(player, None)
        case model.TrumpRevealed(trump)                       => view.TrumpRevealed(trump)
        case model.CardPlayed(player, card)                   => view.CardPlayed(player, card)
        case model.TrickCompleted(player)                     => view.TrickCompleted(player)
        case model.PointsCount(playerIds, points)             => view.PointsCount(playerIds, points)
        case model.MatchCompleted(winners)                    => view.MatchCompleted(winners)
        case model.MatchDraw                                  => view.MatchDraw
        case model.MatchAborted                               => view.MatchAborted
        case model.GameCompleted(winners)                     => view.GameCompleted(winners)
        case model.GameAborted                                => view.GameAborted
        case model.ActionRequest(player)                      => view.ActionRequest(player)
      }

  private def toModel(event: view.FromPlayer): model.Command =
    event match
      case (view.JoinRoom, _)               => model.JoinRoom(me)
      case (view.LeaveRoom, _)              => model.LeaveRoom(me)
      case (view.ActivateRoom(gameType), _) => model.ActivateRoom(me, gameType)
      case (view.ShuffleDeck, seed)         => model.ShuffleDeck(seed)
      case (view.PlayCard(card), _)         => model.PlayCard(me.id, card)

  def publish(me: model.Player, roomId: model.RoomId, events: fs2.Stream[F, view.FromPlayer]): fs2.Stream[F, Unit] =
    events
      .zip(seeds)
      .map(toModel)
      .map(model.Message(roomId, _))
      .through(messageBus.publish)

  def publish1(me: model.Player, roomId: model.RoomId, event: view.FromPlayer): F[Unit] =
    messageBus.publish1(Message(roomId, toModel(event)))
