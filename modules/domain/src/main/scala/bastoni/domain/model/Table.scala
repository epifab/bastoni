package bastoni.domain.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.util.Random

case class Seat[C <: CardView](
  player: Option[PlayerState],
  hand: List[C],
  collected: List[C],
  played: List[C]
)

object Seat:
  given playerView: Codec[Seat[CardPlayerView]] = deriveCodec
  given serverView: Codec[Seat[CardServerView]] = deriveCodec

case class PlayerSeat[C <: CardView](player: PlayerState, hand: List[C], collected: List[C], played: List[C])

trait Table[C <: CardView]:
  type TableView

  def seats: List[Seat[C]]
  def deck: List[C]
  def active: Boolean

  lazy val indexedSeats = seats.zipWithIndex

  protected def toC(card: CardServerView): C
  protected def removeCard(cards: List[C], card: Card): List[C]

  protected def updateWith(seats: List[Seat[C]] = this.seats, deck: List[C] = this.deck, active: Boolean = active): TableView

  protected def publicEventUpdate(message: PublicEvent): TableView =
    message match
      case Event.PlayerJoinedTable(player, targetIndex) =>
        updateWith(
          seats = indexedSeats.map {
            case (seat, index) if targetIndex == index => seat.copy(Some(SittingOut(player)))
            case (whatever, _) => whatever
          }
        )

      case Event.PlayerLeftTable(player, targetIndex) =>
        updateWith(
          seats = indexedSeats.map {
            case (seat, index) if index == targetIndex => seat.copy(player = None)
            case (whatever, _) => whatever
          }
        )

      case Event.GameStarted(_) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(sittingOut: SittingOut), _, _, _) => seat.copy(player = Some(sittingOut.sitIn))
            case whatever => whatever
          },
          active = true
        )

      case Event.TrumpRevealed(card) =>
        updateWith(
          deck = deck match {
            case head :: tail => tail :+ toC(CardServerView(card, Face.Up))
            case whatever => whatever
          }
        )

      case Event.CardPlayed(playerId, card) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(acting: ActingPlayer), _, _, _) if acting.playerId == playerId =>
              seat.copy(
                player = Some(acting.done),
                hand = removeCard(seat.hand, card),
                played = toC(CardServerView(card, Face.Up)) :: seat.played
              )
            case whatever => whatever
          }
        )

      case Event.TrickCompleted(winnerId) =>
        updateWith(
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
        extension (points: List[PointsCount])
          def pointsFor(player: GamePlayer): Option[Int] =
            points.find(_.playerIds.exists(player.is)).map(_.points)

        updateWith(
          seats = seats.map(seat => seat.copy(
            player = seat.player.map {
              case active: SittingIn =>
                EndOfMatchPlayer(
                  gamePlayer = active.gamePlayer.copy(points = gamePoints.pointsFor(active.gamePlayer).getOrElse(active.gamePlayer.points)),
                  points = matchPoints.pointsFor(active.gamePlayer).getOrElse(0),
                  winner = winnerIds.exists(active.gamePlayer.is)
                )
              case whatever => whatever
            },
            hand = Nil,
            collected = Nil,
            played = Nil
          )),
          deck = Nil
        )

      case Event.GameCompleted(winnerIds) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(active: SittingIn), _, _, _) =>
              seat.copy(player = Some(EndOfGamePlayer(active.gamePlayer, winner = winnerIds.contains(active.player.id))))
            case whatever => whatever
          },
          deck = Nil,
          active = false
        )

      case Event.MatchAborted | Event.GameAborted =>
        updateWith(
          seats = seats.map {
            case Seat(Some(player: SittingIn), _, _, _) =>
              Seat(Some(player.sitOut), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil
        )

      case Event.ActionRequested(playerId, Action.ShuffleDeck) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(waiting: SittingIn), _, _, _) if waiting.playerId == playerId =>
              seat.copy(player = Some(waiting.act(Action.ShuffleDeck).mapPlayer(_.copy(dealer = true))))
            case seat@ Seat(Some(sittingIn: SittingIn), _, _, _) =>
              seat.copy(player = Some(sittingIn.mapPlayer(_.copy(dealer = false))))
            case whatever => whatever
          }
        )

      case Event.ActionRequested(playerId, action) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(waiting: SittingIn), _, _, _) if waiting.playerId == playerId =>
              seat.copy(player = Some(waiting.act(action)))
            case whatever => whatever
          }
        )

  protected def cardDealtUpdate(event: Event.CardDealt[C]): TableView =
    updateWith(
      seats = seats.map {
        case seat if seat.player.exists(_.playerId == event.playerId) =>
          seat.copy(hand = event.card :: seat.hand)
        case whatever => whatever
      },
      deck = deck match {
        case head :: tail => tail
        case whatever => whatever
      }
    )

  def seatFor(player: Player): Option[PlayerSeat[C]] =
    seats.collectFirst {
      case Seat(Some(p), hand, collected, played) if p.playerId == player.id =>
        PlayerSeat(p, hand, collected, played)
    }


object Table:
  given serverTableViewCodec: Codec[TableServerView] = deriveCodec
  given playerTableViewCodec: Codec[TablePlayerView] = deriveCodec
