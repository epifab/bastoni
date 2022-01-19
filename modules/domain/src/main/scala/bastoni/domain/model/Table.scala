package bastoni.domain.model

import bastoni.domain.model.PlayerState.*
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.util.Random

case class Seat[C <: CardView](
  player: Option[PlayerState],
  hand: List[C],
  taken: List[C]
)

object Seat:
  given playerView: Codec[Seat[CardPlayerView]] = deriveCodec
  given serverView: Codec[Seat[CardServerView]] = deriveCodec

case class TakenSeat[C <: CardView](player: PlayerState, hand: List[C], taken: List[C])

trait Table[C <: CardView]:
  type TableView

  def seats: List[Seat[C]]
  def deck: List[C]
  def board: List[(Option[UserId], C)]
  def active: Option[GameType]

  lazy val indexedSeats: List[(Seat[C], Int)] = seats.zipWithIndex

  protected def buildCard(card: VisibleCard, direction: Direction): C
  protected def faceDown(card: C): C

  extension[T](list: List[T])
    def removeFirst(cond: T => Boolean): List[T] =
      list match {
        case head :: tail if cond(head) => tail
        case head :: tail => head :: tail.removeFirst(cond)
        case Nil => Nil
      }

  protected def removeCard(cards: List[C], card: VisibleCard): List[C] =
    cards.removeFirst(_.card.ref == card.ref)

  protected def updateWith(
    seats: List[Seat[C]] = this.seats,
    deck: List[C] = this.deck,
    board: List[(Option[UserId], C)] = this.board,
    active: Option[GameType] = active
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

      case Event.GameStarted(gameType) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(sittingOut: SittingOut), _, _) => seat.copy(player = Some(sittingOut.sitIn))
            case seat@ Seat(Some(sittingOut: SittingIn), _, _) => seat.copy(player = Some(sittingOut.sitIn))
            case whatever => whatever
          },
          active = Some(gameType)
        )

      case Event.TrumpRevealed(card) =>
        updateWith(
          deck = deck match {
            case head :: tail => tail :+ buildCard(card, Direction.Up)
            case whatever => whatever
          }
        )

      case Event.BoardCardsDealt(board) =>
        updateWith(board = board.map(c => None -> buildCard(c, Direction.Up)))

      case Event.CardPlayed(playerId, card) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(acting: ActingPlayer), _, _) if acting.is(playerId) =>
              seat.copy(
                player = Some(acting.done),
                hand = removeCard(seat.hand, card)
              )
            case whatever => whatever
          },
          board = Some(playerId) -> buildCard(card, Direction.Up) :: board
        )

      case Event.CardsTaken(playerId, taken, scopa) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(player: SittingIn), _, _) if player.is(playerId) =>
              seat.copy(
                taken = taken.map(card => buildCard(card, if (scopa.contains(card)) Direction.Up else Direction.Down)) ++ seat.taken
              )
            case whatever => whatever
          },
          board = taken.foldLeft(board) {
            case (remainingBoardCards, toRemove) => remainingBoardCards.filterNot {
              case (_, boardCard) => boardCard.card.ref == toRemove.ref
            }
          }
        )

      case Event.TrickCompleted(winnerId) =>
        updateWith(
          seats = seats.map {
            case seat if seat.player.exists(_.is(winnerId)) => seat.copy(taken = seat.taken ++ board.map { case (_, card) => faceDown(card) })
            case whatever => whatever
          },
          board = Nil
        )

      case done: Event.GameCompleted =>
        extension (score: List[Score])
          def pointsFor(player: MatchPlayer): Option[Int] =
            score.find(_.playerIds.exists(player.is)).map(_.points)

        updateWith(
          seats = seats.map(seat => seat.copy(
            player = seat.player.map {
              case active: SittingIn =>
                EndOfGamePlayer(
                  player = active.player.copy(points = done.matchScores.pointsFor(active.player).getOrElse(active.player.points)),
                  points = done.scores.pointsFor(active.player).getOrElse(0),
                  winner = done.scores.bestTeam.exists(active.is)
                )
              case whatever => whatever
            },
            hand = Nil,
            taken = Nil
          )),
          deck = Nil,
          board = Nil
        )

      case Event.MatchCompleted(winnerIds) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(active: SittingIn), _, _) =>
              seat.copy(player = Some(EndOfMatchPlayer(active.player, winner = winnerIds.contains(active.player.id))))
            case whatever => whatever
          },
          active = None
        )

      case Event.GameAborted | Event.MatchAborted =>
        updateWith(
          seats = seats.map {
            case Seat(Some(player: SittingIn), _, _) =>
              Seat(Some(player.sitOut), Nil, Nil)
            case Seat(whatever, _, _) =>
              Seat(whatever, Nil, Nil)
          },
          deck = Nil,
          board = Nil,
          active = None
        )

      case Event.ActionRequested(playerId, Action.ShuffleDeck, timeout) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(player: SittingIn), _, _) if player.is(playerId) =>
              seat.copy(player = Some(player.act(Action.ShuffleDeck, timeout)))
            case whatever => whatever
          }
        )

      case Event.ActionRequested(playerId, action, timeout) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(player: SittingIn), _, _) if player.is(playerId) =>
              seat.copy(player = Some(player.act(action, timeout)))
            case whatever => whatever
          }
        )

      case Event.TimedOut(playerId, _) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(player: ActingPlayer), _, _) if player.is(playerId) =>
              seat.copy(player = Some(player.copy(timeout = Some(Timeout.TimedOut))))
            case whatever => whatever
          }
        )

  protected def cardsDealtUpdate(event: Event.CardsDealt[C]): TableView =
    updateWith(
      seats = seats.map {
        case seat if seat.player.exists(_.is(event.playerId)) =>
          seat.copy(hand = event.cards ++ seat.hand)
        case whatever => whatever
      },
      deck = deck.drop(event.cards.size)
    )

  protected def deckShuffledUpdate(newDeck: List[C]): TableView =
    updateWith(
      seats = seats.map {
        case seat@ Seat(Some(acting@ ActingPlayer(targetPlayer, Action.ShuffleDeck, _)), _, _) =>
          seat.copy(player = Some(acting.done))
        case whatever => whatever
      },
      deck = newDeck
    )

  def seatFor(userId: UserId): Option[TakenSeat[C]] =
    seats.collectFirst {
      case Seat(Some(player), hand, taken) if player.is(userId) =>
        TakenSeat(player, hand, taken)
    }

  def seatFor(user: User): Option[TakenSeat[C]] = seatFor(user.id)


object Table:
  given serverTableViewCodec: Codec[TableServerView] = deriveCodec
  given playerTableViewCodec: Codec[TablePlayerView] = deriveCodec
