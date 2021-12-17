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
    case Event.DeckShuffledServerView(deck) => deckShuffledUpdate(deck.map(card => CardServerView(card, Direction.Down)))

    case event: Event.CardsDealtServerView => cardsDealtUpdate(event)

    case Event.Snapshot(table) => table

    case event: PublicEvent => publicEventUpdate(event)
  }

  def toPlayerView(me: User): TablePlayerView =
    TablePlayerView(
      seats = seats.map {
        case Seat(player, hand, taken, played) =>
          Seat[CardPlayerView](
            player = player,
            hand = hand.map(_.toPlayerView(me.id, player.map(_.id))),
            taken = taken.map(_.toPlayerView(me.id, player.map(_.id))),
            played = played.map(_.toPlayerView(me.id, player.map(_.id)))
          )
      },
      deck = deck.map(_.toPlayerView(me.id, None)),
      board = board.map(_.toPlayerView(me.id, None)),
      active = active
    )

  val players: List[User] = seats.collect { case Seat(Some(seated), _, _, _) => seated }
  val size: Int = seats.size
  val isFull: Boolean = seats.forall(_.player.isDefined)
  val isEmpty: Boolean = seats.forall(_.player.isEmpty)
  val nonEmpty: Boolean = seats.exists(_.player.isDefined)

  def contains(player: User): Boolean = contains(player.id)
  def contains(player: UserId): Boolean = seatIndexFor(player).isDefined

  def seatIndexFor(player: User): Option[Int] = seatIndexFor(player.id)
  def seatIndexFor(id: UserId): Option[Int] = indexedSeats.collectFirst { case (Seat(Some(player), _, _, _), index) if player.is(id) => index }

  def join(player: User, seed: Int): Either[TableError, (TableServerView, Int)] =
    if (contains(player)) Left(TableError.DuplicatePlayer) else {
      new Random(seed)
        .shuffle(indexedSeats)
        .collectFirst { case (Seat(None, _, _, _), index) => index }
        .fold[Either[TableError, (TableServerView, Int)]](Left(TableError.FullTable)) { targetIndex =>
          Right(updateWith(
            seats = indexedSeats.map {
              case (oldSeat, index) if index == targetIndex => oldSeat.copy(player = Some(SittingOut(player)))
              case (seat, _) => seat
            }
          ) -> targetIndex)
        }
    }

  def leave(player: User): Either[TableError, (TableServerView, Int)] =
    seatIndexFor(player) match
      case Some(targetIndex) =>
        Right(copy(
          seats = indexedSeats.map {
            case (oldSeat, index) if index == targetIndex => oldSeat.copy(player = None)
            case (seat, _) => seat
          }
        ) -> targetIndex)

      case None => Left(TableError.PlayerNotFound)
