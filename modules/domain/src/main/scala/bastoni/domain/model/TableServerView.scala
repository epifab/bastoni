package bastoni.domain.model

import scala.util.Random

enum TableError:
  case FullTable, PlayerNotFound, DuplicatePlayer

case class TableServerView(
  override val seats: List[Seat[CardServerView]],
  override val deck: List[CardServerView],
  override val board: List[CardServerView],
  override val active: Boolean
) extends Table[CardServerView]:

  override type TableView = TableServerView

  override protected def updateWith(
    seats: List[Seat[CardServerView]] = this.seats,
    deck: List[CardServerView] = this.deck,
    board: List[CardServerView] = this.board,
    active: Boolean = this.active
  ): TableServerView = TableServerView(seats, deck, board, active)

  override protected def buildCard(card: Card, direction: Direction): CardServerView = CardServerView(card, direction)
  override protected def removeCard(cards: List[CardServerView], card: Card): List[CardServerView] = cards.filterNot(_.card == card)

  def update(event: ServerEvent): TableServerView = event match {
    case Event.DeckShuffledServerView(deck) =>
      updateWith(
        seats = seats.map {
          case seat@ Seat(Some(acting@ ActingPlayer(targetPlayer, Action.ShuffleDeck, _)), _, _, _) =>
            seat.copy(player = Some(acting.done))
          case whatever => whatever
        },
        deck = deck.map(card => CardServerView(card, Direction.Down))
      )

    case event: Event.CardsDealtServerView => cardsDealtUpdate(event)

    case Event.Snapshot(table) => table

    case event: PublicEvent => publicEventUpdate(event)
  }

  def toPlayerView(me: Player): TablePlayerView =
    TablePlayerView(
      seats = seats.map {
        case Seat(player, hand, taken, played) =>
          Seat[CardPlayerView](
            player = player,
            hand = hand.map(_.toPlayerView(me.id, player.map(_.playerId))),
            taken = taken.map(_.toPlayerView(me.id, player.map(_.playerId))),
            played = played.map(_.toPlayerView(me.id, player.map(_.playerId)))
          )
      },
      deck = deck.map(_.toPlayerView(me.id, None)),
      board = board.map(_.toPlayerView(me.id, None)),
      active = active
    )

  val players: List[Player] = seats.collect { case Seat(Some(seated), _, _, _) => seated.player }
  val size: Int = seats.size
  val isFull: Boolean = seats.forall(_.player.isDefined)
  val isEmpty: Boolean = seats.forall(_.player.isEmpty)
  val nonEmpty: Boolean = seats.exists(_.player.isDefined)

  def contains(player: Player): Boolean = contains(player.id)
  def contains(player: PlayerId): Boolean = seatIndexFor(player).isDefined

  def seatIndexFor(player: Player): Option[Int] = seatIndexFor(player.id)
  def seatIndexFor(id: PlayerId): Option[Int] = indexedSeats.collectFirst { case (Seat(Some(player), _, _, _), index) if player.playerId == id => index }

  def join(p: Player, seed: Int): Either[TableError, (TableServerView, Int)] =
    if (contains(p)) Left(TableError.DuplicatePlayer) else {
      new Random(seed)
        .shuffle(indexedSeats)
        .collectFirst { case (Seat(None, _, _, _), index) => index }
        .fold[Either[TableError, (TableServerView, Int)]](Left(TableError.FullTable)) { targetIndex =>
          Right(updateWith(
            seats = indexedSeats.map {
              case (oldSeat, index) if index == targetIndex => oldSeat.copy(player = Some(SittingOut(p)))
              case (seat, _) => seat
            }
          ) -> targetIndex)
        }
    }

  def leave(p: Player): Either[TableError, (TableServerView, Int)] =
    seatIndexFor(p) match
      case Some(targetIndex) =>
        Right(copy(
          seats = indexedSeats.map {
            case (oldSeat, index) if index == targetIndex => oldSeat.copy(player = None)
            case (seat, _) => seat
          }
        ) -> targetIndex)

      case None => Left(TableError.PlayerNotFound)
