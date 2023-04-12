package bastoni.domain.model

import bastoni.domain.model.PlayerState.*

import scala.util.Random

enum RoomError:
  case FullRoom, PlayerNotFound, DuplicatePlayer

case class RoomServerView(
    override val seats: List[Seat[CardServerView]],
    override val deck: List[CardServerView],
    override val board: List[BoardCard[CardServerView]],
    override val matchInfo: Option[MatchInfo],
    override val dealerIndex: Option[Int],
    override val players: Map[UserId, User]
) extends Room[CardServerView]:

  override type RoomView = RoomServerView

  override protected def updateWith(
      seats: List[Seat[CardServerView]],
      deck: List[CardServerView] = this.deck,
      board: List[BoardCard[CardServerView]] = this.board,
      matchInfo: Option[MatchInfo] = this.matchInfo,
      dealerIndex: Option[Int] = this.dealerIndex
  ): RoomServerView = RoomServerView(
    seats = seats,
    deck = deck,
    board = board,
    matchInfo = matchInfo,
    dealerIndex = dealerIndex,
    players = updatedPlayers(seats)
  )

  override protected def buildCard(card: VisibleCard, direction: Direction): CardServerView =
    CardServerView(card, direction)
  override protected def faceDown(card: CardServerView): CardServerView = card.copy(facing = Direction.Down)

  def update(event: ServerEvent): RoomServerView = event match
    case Event.ServerOnlyEvent.DeckShuffled(deck) =>
      deckShuffledUpdate(deck.map(card => CardServerView(card, Direction.Down)))

    case event: Event.ServerOnlyEvent.CardsDealt => cardsDealtUpdate(event)

    case Event.PlayerConnected(_, room) => room

    case event: PublicEvent => publicEventUpdate(event)

  def toPlayerView(me: UserId): RoomPlayerView =
    RoomPlayerView(
      me,
      seats = seats.map { seat =>
        val hand  = seat.hand.map(_.toPlayerView(me, seat.playerOption.map(_.id)))
        val taken = seat.taken.map(_.toPlayerView(me, seat.playerOption.map(_.id)))
        seat match
          case seat: TakenSeat[CardServerView] => seat.copy(hand = hand, taken = taken)
          case seat: EmptySeat[CardServerView] => seat.copy(hand = hand, taken = taken)
      },
      deck = deck.map(_.toPlayerView(me, None)),
      board = board.map(boardCard => BoardCard(boardCard.card.toPlayerView(me, None), boardCard.playedBy)),
      matchInfo = matchInfo,
      dealerIndex = dealerIndex,
      players = players
    )

  val isFull: Boolean   = seats.forall(_.playerOption.isDefined)
  val isEmpty: Boolean  = seats.forall(_.playerOption.isEmpty)
  val nonEmpty: Boolean = seats.exists(_.playerOption.isDefined)

  def contains(player: User): Boolean   = contains(player.id)
  def contains(player: UserId): Boolean = seatFor(player).isDefined

  def join(player: User, seed: Int): Either[RoomError, (RoomServerView, Int)] =
    if (contains(player)) Left(RoomError.DuplicatePlayer)
    else
      new Random(seed)
        .shuffle(seats)
        .collectFirst { case seat if seat.playerOption.isEmpty => seat.index }
        .fold[Either[RoomError, (RoomServerView, Int)]](Left(RoomError.FullRoom)) { targetIndex =>
          Right(
            updateWith(
              seats = seats.map {
                case oldSeat if oldSeat.index == targetIndex => oldSeat.occupiedBy(SittingOut(player))
                case seat                                    => seat
              },
              dealerIndex = dealerIndex.orElse(Some(targetIndex))
            ) -> targetIndex
          )
        }

  def leave(player: User): Either[RoomError, (RoomServerView, Int)] =
    seatFor(player) match
      case Some(seat) =>
        Right(
          copy(
            seats = seats.map {
              case `seat` => seat.vacant
              case seat   => seat
            },
            dealerIndex = if (dealerIndex.contains(seat.index)) None else dealerIndex
          ) -> seat.index
        )

      case None => Left(RoomError.PlayerNotFound)
end RoomServerView
