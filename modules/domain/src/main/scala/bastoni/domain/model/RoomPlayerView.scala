package bastoni.domain.model

case class RoomPlayerView(
    me: UserId,
    override val seats: List[Seat[CardPlayerView]],
    override val deck: List[CardPlayerView],
    override val board: List[BoardCard[CardPlayerView]],
    override val matchInfo: Option[MatchInfo],
    override val dealerIndex: Option[Int],
    override val players: Map[UserId, User]
) extends Room[CardPlayerView]:

  override type RoomView = RoomPlayerView

  override protected def updateWith(
      seats: List[Seat[CardPlayerView]] = this.seats,
      deck: List[CardPlayerView] = this.deck,
      board: List[BoardCard[CardPlayerView]] = this.board,
      matchInfo: Option[MatchInfo] = this.matchInfo,
      dealerIndex: Option[Int] = this.dealerIndex
  ): RoomPlayerView = RoomPlayerView(
    me = me,
    seats = seats,
    deck = deck,
    board = board,
    matchInfo = matchInfo,
    dealerIndex = dealerIndex,
    players = updatedPlayers(seats)
  )

  override protected def buildCard(card: VisibleCard, direction: Direction): CardPlayerView = CardPlayerView(
    direction match
      case Direction.Up => card
      case _            => card.hide
  )

  override protected def faceDown(card: CardPlayerView): CardPlayerView = card.copy(card = card.card.hide)

  def update(event: PlayerEvent): RoomPlayerView = event match
    case Event.PlayerOnlyEvent.DeckShuffled(numberOfCards) =>
      deckShuffledUpdate(
        (0 until numberOfCards).toList
          .map(position => CardPlayerView(HiddenCard(CardId(position))))
      )

    case event: Event.PlayerOnlyEvent.CardsDealt => cardsDealtUpdate(event)

    case event: PublicEvent => publicEventUpdate(event)

  val opponents: List[OccupiedSeat[CardPlayerView]] =
    seats
      .slideUntil(_.playerOption.exists(_.is(me)))
      .filterNot(_.playerOption.exists(_.is(me)))
      .collect { case taken: OccupiedSeat[CardPlayerView] => taken }

  val opponentLeft: Option[OccupiedSeat[CardPlayerView]] = opponents match
    case left :: _ :: _ :: Nil => Some(left)
    case left :: _ :: Nil      => Some(left)
    case _                     => None
  val opponentFront: Option[OccupiedSeat[CardPlayerView]] = opponents match
    case _ :: center :: _ :: Nil => Some(center)
    case center :: Nil           => Some(center)
    case _                       => None
  val opponentRight: Option[OccupiedSeat[CardPlayerView]] = opponents match
    case _ :: _ :: right :: Nil => Some(right)
    case _ :: right :: Nil      => Some(right)
    case _                      => None
  val mainPlayer: Option[OccupiedSeat[CardPlayerView]] = seatFor(me)
end RoomPlayerView
