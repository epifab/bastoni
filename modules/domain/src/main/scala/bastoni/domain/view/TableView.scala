package bastoni.domain.view

import bastoni.domain.model.*
import bastoni.domain.view.ToPlayer.*

case class SeatView(player: Option[PlayerState], hand: List[Option[Card]], collected: List[Option[Card]], played: List[Option[Card]])

case class TableView(seats: List[SeatView], deck: List[Option[Card]], active: Boolean):

  extension[T](xs: List[T])
    def removeFirst(cond: T => Boolean): List[T] =
      xs match {
        case head :: tail if cond(head) => tail
        case head :: tail => head :: tail.removeFirst(cond)
        case Nil => Nil
      }

  def seatFor(player: Player): Option[SeatView] =
    seats.find(_.player.exists(_.playerId == player.id))

  def update(message: ToPlayer): TableView =
    message match {
      case PlayerJoined(player, room) =>
        copy(
          seats = seats.zip(room.seats).map {
            case (seat, Some(targetPlayer)) if targetPlayer.id == player.id => seat.copy(Some(SittingOut(player)))
            case (whatever, _) => whatever
          }
        )

      case PlayerLeft(player, room) =>
        copy(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == player.id) => seat.copy(player = None)
            case whatever => whatever
          }
        )

      case GameStarted(_) => copy(active = true)

      case DeckShuffled(cards) => copy(deck = List.fill(cards)(None))

      case CardDealt(playerId, card) =>
        copy(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == playerId) => seat.copy(hand = card :: seat.hand)
            case whatever => whatever
          }
        )

      case TrumpRevealed(card) =>
        copy(deck = deck.tail :+ Some(card))

      case CardPlayed(playerId, card) =>
        copy(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == playerId) =>
              seat.copy(
                hand =
                  if (seat.hand.exists(_.contains(card))) seat.hand.removeFirst(_.contains(card))
                  else seat.hand.removeFirst(_.isEmpty),
                played = Some(card) :: seat.played
              )
            case whatever => whatever
          }
        )

      case TrickCompleted(winnerId) =>
        copy(seats = seats.map {
          case seat if seat.player.exists(_.playerId == winnerId) =>
            seat.copy(
              collected = List.fill(seats.map(_.played.size).sum)(None) ++ seat.collected,
              played = Nil
            )
          case seat => seat.copy(played = Nil)
        })

      case MatchCompleted(winnerIds, matchPoints, gamePoints) =>
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

      case GameCompleted(winnerIds) =>
        copy(
          seats = seats.map {
            case seat@ SeatView(Some(active: SittingIn), _, _, _) =>
              seat.copy(player = Some(EndOfGamePlayer(active.player, winner = winnerIds.contains(active.player.id))))
            case whatever => whatever
          },
          active = false
        )

      case MatchAborted | GameAborted =>
        copy(
          seats = seats.map {
            case SeatView(Some(p: SittingIn), _, _, _) =>
              SeatView(Some(SittingOut(p.player.player)), Nil, Nil, Nil)
            case SeatView(whatever, _, _, _) =>
              SeatView(whatever, Nil, Nil, Nil)
          },
          active = false,
          deck = Nil
        )

      // case Snapshot(table) => this

      case ActionRequest(playerId, _) =>
        copy(
          seats = seats.map {
            case seat@ SeatView(Some(player: SittingIn), _, _, _) if player.playerId == playerId =>
              seat.copy(player = Some(ActingPlayer(player.player)))
            case whatever => whatever
          }
        )
    }


object TableView:
  extension (state: CardState)
    def toOption(me: Player, player: Option[PlayerState]): Option[Card] = state match {
      case CardState(card, Face.Up) => Some(card)
      case CardState(card, Face.Down) => None
      case CardState(card, Face.Player) => Option.when(player.exists(_.playerId == me.id))(card)
    }

  def apply(me: Player, table: Table): TableView =
    new TableView(
      seats = table.seats.map {
        case Seat(player, hand, collected, played) =>
          SeatView(
            player = player,
            hand = hand.map(_.toOption(me, player)),
            collected = collected.map(_.toOption(me, player)),
            played = played.map(_.toOption(me, player))
          )
      },
      deck = table.deck.map(_.toOption(me, None)),
      active = table.active
    )

  def stream[F[_]](input: fs2.Stream[F, ToPlayer]): fs2.Stream[F, (ToPlayer, TableView)] =
    input
      .zipWithScan[Option[TableView]](None) {
        case (Some(table), message) => Some(table.update(message))
        // case (None, ToPlayer.Snapshot(table)) => Some(table)
        case (None, event: ToPlayer.RoomMessage) =>
          Some(TableView(
            seats = event.room.seats.map(seat =>
              SeatView(
                seat.map(SittingOut(_)),
                hand = Nil,
                collected = Nil,
                played = Nil
              )
            ),
            deck = Nil,
            active = false
          ))
        case (None, _) => None
      }
      .collect { case (message, Some(table)) => message -> table }
