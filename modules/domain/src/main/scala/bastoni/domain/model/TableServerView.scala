package bastoni.domain.model

import bastoni.domain.model.PlayerState.*

import scala.util.Random

enum TableError:
  case FullTable, PlayerNotFound, DuplicatePlayer

case class TableServerView(
  override val seats: List[Seat[CardServerView]],
  override val deck: List[CardServerView],
  override val board: List[(Option[UserId], CardServerView)],
  override val matchInfo: Option[MatchInfo],
  override val dealerIndex: Option[Int]
) extends Table[CardServerView]:

  override type TableView = TableServerView

  override protected def updateWith(
    seats: List[Seat[CardServerView]] = this.seats,
    deck: List[CardServerView] = this.deck,
    board: List[(Option[UserId], CardServerView)] = this.board,
    matchInfo: Option[MatchInfo] = this.matchInfo,
    dealerIndex: Option[Int] = this.dealerIndex
  ): TableServerView = TableServerView(seats, deck, board, matchInfo, dealerIndex)

  override protected def buildCard(card: VisibleCard, direction: Direction): CardServerView = CardServerView(card, direction)
  override protected def faceDown(card: CardServerView): CardServerView = card.copy(facing = Direction.Down)

  def update(event: ServerEvent): TableServerView = event match {
    case Event.DeckShuffledServerView(cards) => deckShuffledUpdate(cards.map(card => CardServerView(card, Direction.Down)))

    case event: Event.CardsDealtServerView => cardsDealtUpdate(event)

    case Event.Snapshot(table) => table

    case event: PublicEvent => publicEventUpdate(event)
  }

  def toPlayerView(me: User): TablePlayerView =
    TablePlayerView(
      me.id,
      seats = seats.map { seat =>
        val hand = seat.hand.map(_.toPlayerView(me.id, seat.playerOption.map(_.id)))
        val taken = seat.taken.map(_.toPlayerView(me.id, seat.playerOption.map(_.id)))
        seat match {
          case seat: TakenSeat[CardServerView] => seat.copy(hand = hand, taken = taken)
          case seat: EmptySeat[CardServerView] => seat.copy(hand = hand, taken = taken)
        }
      },
      deck = deck.map(_.toPlayerView(me.id, None)),
      board = board.map { case (u, c) => u -> c.toPlayerView(me.id, None) },
      matchInfo = matchInfo,
      dealerIndex = dealerIndex
    )

  val isFull: Boolean = seats.forall(_.playerOption.isDefined)
  val isEmpty: Boolean = seats.forall(_.playerOption.isEmpty)
  val nonEmpty: Boolean = seats.exists(_.playerOption.isDefined)

  def contains(player: User): Boolean = contains(player.id)
  def contains(player: UserId): Boolean = seatFor(player).isDefined

  def join(player: User, seed: Int): Either[TableError, (TableServerView, Int)] =
    if (contains(player)) Left(TableError.DuplicatePlayer) else {
      new Random(seed)
        .shuffle(seats)
        .collectFirst { case seat if seat.playerOption.isEmpty => seat.index }
        .fold[Either[TableError, (TableServerView, Int)]](Left(TableError.FullTable)) { targetIndex =>
          Right(updateWith(
            seats = seats.map {
              case oldSeat if oldSeat.index == targetIndex => oldSeat.occupiedBy(SittingOut(player))
              case seat => seat
            },
            dealerIndex = dealerIndex.orElse(Some(targetIndex))
          ) -> targetIndex)
        }
    }

  def leave(player: User): Either[TableError, (TableServerView, Int)] =
    seatFor(player) match
      case Some(seat) =>
        Right(copy(
          seats = seats.map {
            case `seat` => seat.vacant
            case seat => seat
          },
          dealerIndex = if (dealerIndex.contains(seat.index)) None else dealerIndex
        ) -> seat.index)

      case None => Left(TableError.PlayerNotFound)
