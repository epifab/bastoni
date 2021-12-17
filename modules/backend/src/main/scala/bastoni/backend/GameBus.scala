package bastoni.backend

import bastoni.domain.{model, view}

class GameBus[F[_]](messageBus: MessageBus[F], seeds: fs2.Stream[F, Int]):

  def subscribe(me: model.Player, roomId: model.RoomId): fs2.Stream[F, view.Event] =
    messageBus
      .subscribe
      .collect { case model.Message(`roomId`, event: model.Event) => event }
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
      }

  def publish(me: model.Player, roomId: model.RoomId, events: fs2.Stream[F, view.Command]): fs2.Stream[F, Unit] =
    events
      .zip(seeds)
      .map {
        case (view.JoinRoom, _)               => model.Message(roomId, model.JoinRoom(me))
        case (view.LeaveRoom, _)              => model.Message(roomId, model.LeaveRoom(me))
        case (view.ActivateRoom(gameType), _) => model.Message(roomId, model.ActivateRoom(me, gameType))
        case (view.ShuffleDeck, seed)         => model.Message(roomId, model.ShuffleDeck(seed))
        case (view.PlayCard(card), _)         => model.Message(roomId, model.PlayCard(me.id, card))
      }
      .through(messageBus.publish)
