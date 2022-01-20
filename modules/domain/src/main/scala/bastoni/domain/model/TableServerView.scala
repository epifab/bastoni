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
      seats = seats.map {
        case Seat(index, player, hand, taken) =>
          Seat[CardPlayerView](
            index = index,
            player = player,
            hand = hand.map(_.toPlayerView(me.id, player.map(_.id))),
            taken = taken.map(_.toPlayerView(me.id, player.map(_.id)))
          )
      },
      deck = deck.map(_.toPlayerView(me.id, None)),
      board = board.map { case (u, c) => u -> c.toPlayerView(me.id, None) },
      matchInfo = matchInfo,
      dealerIndex = dealerIndex
    )

  val players: List[User] = seats.flatMap(_.player)
  val size: Int = seats.size
  val isFull: Boolean = seats.forall(_.player.isDefined)
  val isEmpty: Boolean = seats.forall(_.player.isEmpty)
  val nonEmpty: Boolean = seats.exists(_.player.isDefined)

  def contains(player: User): Boolean = contains(player.id)
  def contains(player: UserId): Boolean = seatIndexFor(player).isDefined

  def seatIndexFor(player: User): Option[Int] = seatIndexFor(player.id)
  def seatIndexFor(id: UserId): Option[Int] = seats.collectFirst { case Seat(index, Some(player), _, _) if player.is(id) => index }

  def join(player: User, seed: Int): Either[TableError, (TableServerView, Int)] =
    if (contains(player)) Left(TableError.DuplicatePlayer) else {
      new Random(seed)
        .shuffle(seats)
        .collectFirst { case Seat(index, None, _, _) => index }
        .fold[Either[TableError, (TableServerView, Int)]](Left(TableError.FullTable)) { targetIndex =>
          Right(updateWith(
            seats = seats.map {
              case oldSeat if oldSeat.index == targetIndex => oldSeat.copy(player = Some(SittingOut(player)))
              case seat => seat
            },
            dealerIndex = dealerIndex.orElse(Some(targetIndex))
          ) -> targetIndex)
        }
    }

  def leave(player: User): Either[TableError, (TableServerView, Int)] =
    seatIndexFor(player) match
      case Some(targetIndex) =>
        Right(copy(
          seats = seats.map {
            case oldSeat if oldSeat.index == targetIndex => oldSeat.copy(player = None)
            case seat => seat
          },
          dealerIndex = if (dealerIndex.contains(targetIndex)) None else dealerIndex
        ) -> targetIndex)

      case None => Left(TableError.PlayerNotFound)
