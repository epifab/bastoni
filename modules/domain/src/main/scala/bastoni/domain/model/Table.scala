package bastoni.domain.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Seat(
  player: Option[PlayerState],
  hand: List[CardState],
  collected: List[CardState],
  played: List[CardState]
)

object Seat:
  given Codec[Seat] = deriveCodec

case class Table(seats: List[Seat], deck: List[CardState], active: Boolean):

  def update(message: Event | Command): Table =
    message match
      case Event.PlayerJoined(player, room) =>
        copy(
          seats = seats.zip(room.seats).map {
            case (seat, Some(targetPlayer)) if targetPlayer.id == player.id => seat.copy(Some(SittingOut(player)))
            case (whatever, _) => whatever
          }
        )

      case Event.PlayerLeft(player, room) =>
        copy(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == player.id) => seat.copy(player = None)
            case whatever => whatever
          }
        )

      case Event.GameStarted(_) =>
        copy(
          seats = seats.map {
            case seat@ Seat(Some(sittingOut: SittingOut), _, _, _) => seat.copy(player = Some(sittingOut.sitIn))
            case whatever => whatever
          },
          active = true
        )

      case Event.DeckShuffled(deck) =>
        copy(
          seats = seats.map {
            case seat@ Seat(Some(acting@ ActingPlayer(targetPlayer, Action.ShuffleDeck)), _, _, _) =>
              seat.copy(player = Some(acting.done))
            case whatever => whatever
          },
          deck = deck.map(card => CardState(card, Face.Down))
        )

      case Event.TrumpRevealed(card) =>
        copy(
          deck = deck match {
            case head :: tail if head.card == card => tail :+ CardState(card, Face.Up)
            case whatever => whatever
          }
        )

      case Event.CardDealt(playerId, card, face) =>
        copy(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == playerId) && deck.headOption.exists(_.card == card) =>
              seat.copy(hand = CardState(card, face) :: seat.hand)
            case whatever => whatever
          },
          deck = deck match {
            case head :: tail if head.card == card => tail
            case whatever => whatever
          }
        )

      case Event.CardPlayed(playerId, card) =>
        copy(
          seats = seats.map {
            case seat@ Seat(Some(acting: ActingPlayer), _, _, _) if acting.playerId == playerId =>
              seat.copy(
                player = Some(acting.done),
                hand = seat.hand.filterNot(_.card == card),
                played = CardState(card, Face.Up) :: seat.played
              )
            case whatever => whatever
          }
        )

      case Event.TrickCompleted(winnerId) =>
        copy(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == winnerId) =>
              seat.copy(
                collected = seat.collected ++ seats.flatMap(_.played),
                played = Nil
              )
            case seat => seat.copy(played = Nil)
          }
        )

      case Event.MatchCompleted(winnerIds, matchPoints, gamePoints) =>
        copy(
          seats = seats.map(seat => seat.copy(
            player = seat.player.map {
              case active: SittingIn =>
                val playerGamePoints = gamePoints.find(_.playerIds.exists(active.player.is)).map(_.points).getOrElse(active.player.points)
                val playerMatchPoints = matchPoints.find(_.playerIds.exists(active.player.is)).map(_.points).getOrElse(0)
                EndOfMatchPlayer(active.player.copy(points = playerGamePoints), playerMatchPoints, winnerIds.exists(active.player.is))
              case whatever => whatever
            },
            hand = Nil,
            collected = Nil,
            played = Nil
          )),
          deck = Nil
        )

      case Event.GameCompleted(winnerIds) =>
        copy(
          seats = seats.map {
            case seat@ Seat(Some(active: SittingIn), _, _, _) =>
              seat.copy(player = Some(EndOfGamePlayer(active.player, winner = winnerIds.contains(active.player.id))))
            case whatever => whatever
          },
          deck = Nil,
          active = false
        )

      case Event.MatchAborted | Event.GameAborted =>
        copy(
          seats = seats.map {
            case Seat(Some(player: SittingIn), _, _, _) =>
              Seat(Some(player.sitOut), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil
        )

      case Command.ActionRequest(playerId, action) =>
        copy(
          seats = seats.map {
            case Seat(Some(waiting: SittingIn), hand, collected, played) if waiting.playerId == playerId =>
              Seat(Some(waiting.act(action)), hand, collected, played)
            case whatever => whatever
          }
        )

      case _: Command => this


object Table:
  given Codec[Table] = deriveCodec

  def apply(message: Event | Command): Option[Table] =
    val room = message match {
      case event: Event.RoomEvent => Some(event.room)
      case command: Command.StartGame => Some(command.room)
      case _ => None
    }

    room.map { room =>
      Table(
        seats = room.seats.map(seat =>
          Seat(
            seat.map(SittingOut(_)),
            hand = Nil,
            collected = Nil,
            played = Nil
          )
        ),
        deck = Nil,
        active = false
      )
    }
