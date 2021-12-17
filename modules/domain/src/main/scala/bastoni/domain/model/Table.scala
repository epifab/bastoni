package bastoni.domain.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.util.Random

case class Seat[C <: CardView](
  player: Option[PlayerState],
  hand: List[C],
  taken: List[C],
  played: List[C]
)

object Seat:
  given playerView: Codec[Seat[CardPlayerView]] = deriveCodec
  given serverView: Codec[Seat[CardServerView]] = deriveCodec

case class PlayerSeat[C <: CardView](player: PlayerState, hand: List[C], taken: List[C], played: List[C])

trait Table[C <: CardView]:
  type TableView

  def seats: List[Seat[C]]
  def deck: List[C]
  def board: List[C]
  def active: Boolean

  lazy val indexedSeats = seats.zipWithIndex

  protected def buildCard(card: Card, direction: Direction): C
  protected def removeCard(cards: List[C], card: Card): List[C]

  protected def updateWith(
    seats: List[Seat[C]] = this.seats,
    deck: List[C] = this.deck,
    board: List[C] = this.board,
    active: Boolean = active
  ): TableView

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
            case head :: tail => tail :+ buildCard(card, Direction.Up)
            case whatever => whatever
          }
        )

      case Event.BoardCardsDealt(board) =>
        updateWith(board = board.map(buildCard(_, Direction.Up)))

      case Event.CardPlayed(playerId, card) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(acting: ActingPlayer), _, _, _) if acting.playerId == playerId =>
              seat.copy(
                player = Some(acting.done),
                hand = removeCard(seat.hand, card),
                played = buildCard(card, Direction.Up) :: seat.played
              )
            case whatever => whatever
          }
        )

      case Event.CardsTaken(playerId, played, taken, extraPoint) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(acting: ActingPlayer), _, _, _) if acting.playerId == playerId =>
              seat.copy(
                player = Some(acting.done),
                hand = removeCard(seat.hand, played),
                taken =
                  if (taken.isEmpty) seat.taken
                  else (taken.map(buildCard(_, Direction.Down)) ++ (buildCard(played, if (extraPoint) Direction.Up else Direction.Down) :: seat.taken))
              )
            case whatever => whatever
          },
          board =
            if (taken.isEmpty) (buildCard(played, Direction.Up) :: board)
            else taken.foldLeft(board)(removeCard)
        )

      case Event.TrickCompleted(winnerId) =>
        updateWith(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == winnerId) =>
              seat.copy(
                taken = seat.taken ++ seats.flatMap(_.played),
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
            taken = Nil,
            played = Nil
          )),
          deck = Nil,
          board = Nil
        )

      case Event.GameCompleted(winnerIds) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(active: SittingIn), _, _, _) =>
              seat.copy(player = Some(EndOfGamePlayer(active.gamePlayer, winner = winnerIds.contains(active.player.id))))
            case whatever => whatever
          },
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
          deck = Nil,
          board = Nil,
          active = false
        )

      case Event.ActionRequested(playerId, Action.ShuffleDeck, timeout) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(waiting: SittingIn), _, _, _) if waiting.playerId == playerId =>
              seat.copy(player = Some(waiting.act(Action.ShuffleDeck, timeout).mapPlayer(_.copy(dealer = true))))
            case seat@ Seat(Some(sittingIn: SittingIn), _, _, _) =>
              seat.copy(player = Some(sittingIn.mapPlayer(_.copy(dealer = false))))
            case whatever => whatever
          }
        )

      case Event.ActionRequested(playerId, action, timeout) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(p: SittingIn), _, _, _) if p.playerId == playerId =>
              seat.copy(player = Some(p.act(action, timeout)))
            case whatever => whatever
          }
        )

      case Event.TimedOut(playerId, _) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(p: ActingPlayer), _, _, _) if p.playerId == playerId =>
              seat.copy(player = Some(p.copy(timeout = Some(Timeout.TimedOut))))
            case whatver => whatver
          }
        )


  protected def cardsDealtUpdate(event: Event.CardsDealt[C]): TableView =
    updateWith(
      seats = seats.map {
        case seat if seat.player.exists(_.playerId == event.playerId) =>
          seat.copy(hand = event.cards ++ seat.hand)
        case whatever => whatever
      },
      deck = deck match {
        case head :: tail => tail
        case whatever => whatever
      }
    )

  def seatFor(player: Player): Option[PlayerSeat[C]] =
    seats.collectFirst {
      case Seat(Some(p), hand, taken, played) if p.playerId == player.id =>
        PlayerSeat(p, hand, taken, played)
    }


object Table:
  given serverTableViewCodec: Codec[TableServerView] = deriveCodec
  given playerTableViewCodec: Codec[TablePlayerView] = deriveCodec
