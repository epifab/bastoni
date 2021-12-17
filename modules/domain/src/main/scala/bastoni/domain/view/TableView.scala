package bastoni.domain.view

import bastoni.domain.model.*
import bastoni.domain.view.ToPlayer.*

case class SeatView(player: Option[PlayerState], hand: List[Option[Card]], collected: List[Option[Card]], played: List[Option[Card]])

case class PlayerSeat(player: PlayerState, hand: List[Option[Card]], collected: List[Option[Card]], played: List[Option[Card]])

case class TableView(seats: List[SeatView], deck: List[Option[Card]], active: Boolean):
  def seatFor(player: Player): Option[PlayerSeat] =
    seats.collectFirst {
      case SeatView(Some(p), hand, collected, played) if p.playerId == player.id =>
        PlayerSeat(p, hand, collected, played)
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
