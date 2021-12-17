package bastoni.domain.model

// import bastoni.domain.view.ToPlayer.Snapshot
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
        copy(active = true)

      case Event.DeckShuffled(deck) =>
        copy(
          seats = seats.map {
            case (Seat(Some(SittingOut(p)), _, _, _)) => Seat(Some(WatingPlayer(GamePlayer(p, 0))), Nil, Nil, Nil)
            case (Seat(Some(p: SittingIn), _, _, _)) => Seat(Some(WatingPlayer(p.player)), Nil, Nil, Nil)
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
            case seat if seat.player.exists(_.playerId == playerId) && seat.hand.exists(_.card == card) =>
              seat.copy(
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
            }
          )),
          deck = Nil
        )

      case Event.GameCompleted(winnerIds) =>
        copy(
          seats = seats.map {
            case Seat(Some(active: SittingIn), _, _, _) =>
              Seat(Some(EndOfGamePlayer(active.player, winner = winnerIds.contains(active.player.id))), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil,
          active = false
        )

      case Event.MatchAborted | Event.GameAborted =>
        copy(
          seats = seats.map {
            case Seat(Some(p: SittingIn), _, _, _) =>
              Seat(Some(SittingOut(p.player.player)), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil
        )

      case Command.ActionRequest(playerId, _) =>
        copy(
          seats = seats.map {
            case Seat(Some(WatingPlayer(player)), hand, collected, played) if player.id == playerId =>
              Seat(Some(ActingPlayer(player)), hand, collected, played)
            case whatever => whatever
          }
        )

      // case Event.Snapshot(_) => this

      case _: Command => this


object Table:
  given Codec[Table] = deriveCodec
