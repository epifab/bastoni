package bastoni.domain.model

import bastoni.domain.model.PlayerState.*

import scala.util.Random

enum RoomError:
  case FullRoom, PlayerNotFound, DuplicatePlayer

case class RoomServerView(
  override val seats: List[Seat[CardServerView]],
  override val deck: List[CardServerView],
  override val board: List[(Option[UserId], CardServerView)],
  override val matchInfo: Option[MatchInfo],
  override val dealerIndex: Option[Int]
) extends Room[CardServerView]:

  override type RoomView = RoomServerView

  override protected def updateWith(
    seats: List[Seat[CardServerView]] = this.seats,
    deck: List[CardServerView] = this.deck,
    board: List[(Option[UserId], CardServerView)] = this.board,
    matchInfo: Option[MatchInfo] = this.matchInfo,
    dealerIndex: Option[Int] = this.dealerIndex
  ): RoomServerView = RoomServerView(seats, deck, board, matchInfo, dealerIndex)

  override protected def buildCard(card: VisibleCard, direction: Direction): CardServerView = CardServerView(card, direction)
  override protected def faceDown(card: CardServerView): CardServerView = card.copy(facing = Direction.Down)

  def update(event: ServerEvent): RoomServerView = event match {
    case Event.DeckShuffledServerView(deck) => deckShuffledUpdate(deck.map(card => CardServerView(card, Direction.Down)))

    case event: Event.CardsDealtServerView => cardsDealtUpdate(event)

    case Event.Snapshot(room) => room

    case event: PublicEvent => publicEventUpdate(event)
  }

  def toPlayerView(me: User): RoomPlayerView =
    RoomPlayerView(
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

  def join(player: User, seed: Int): Either[RoomError, (RoomServerView, Int)] =
    if (contains(player)) Left(RoomError.DuplicatePlayer) else {
      new Random(seed)
        .shuffle(seats)
        .collectFirst { case seat if seat.playerOption.isEmpty => seat.index }
        .fold[Either[RoomError, (RoomServerView, Int)]](Left(RoomError.FullRoom)) { targetIndex =>
          Right(updateWith(
            seats = seats.map {
              case oldSeat if oldSeat.index == targetIndex => oldSeat.occupiedBy(SittingOut(player))
              case seat => seat
            },
            dealerIndex = dealerIndex.orElse(Some(targetIndex))
          ) -> targetIndex)
        }
    }

  def leave(player: User): Either[RoomError, (RoomServerView, Int)] =
    seatFor(player) match
      case Some(seat) =>
        Right(copy(
          seats = seats.map {
            case `seat` => seat.vacant
            case seat => seat
          },
          dealerIndex = if (dealerIndex.contains(seat.index)) None else dealerIndex
        ) -> seat.index)

      case None => Left(RoomError.PlayerNotFound)