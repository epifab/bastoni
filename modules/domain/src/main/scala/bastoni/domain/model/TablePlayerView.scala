package bastoni.domain.model

case class TablePlayerView(
  me: UserId,
  override val seats: List[Seat[CardPlayerView]],
  override val deck: List[CardPlayerView],
  override val board: List[(Option[UserId], CardPlayerView)],
  override val matchInfo: Option[MatchInfo],
  override val dealerIndex: Option[Int]
) extends Table[CardPlayerView]:

  override type TableView = TablePlayerView

  override protected def updateWith(
    seats: List[Seat[CardPlayerView]] = this.seats,
    deck: List[CardPlayerView] = this.deck,
    board: List[(Option[UserId], CardPlayerView)] = this.board,
    matchInfo: Option[MatchInfo] = this.matchInfo,
    dealerIndex: Option[Int] = this.dealerIndex
  ): TablePlayerView = TablePlayerView(me, seats, deck, board, matchInfo, dealerIndex)

  override protected def buildCard(card: VisibleCard, direction: Direction): CardPlayerView = CardPlayerView(direction match {
    case Direction.Up => card
    case _ => card.hidden
  })

  override protected def faceDown(card: CardPlayerView): CardPlayerView = card.copy(card = card.card.hidden)

  def update(event: PlayerEvent): TablePlayerView = event match {
    case Event.DeckShuffledPlayerView(numberOfCards) =>
      deckShuffledUpdate(
        (0 until numberOfCards)
          .toList
          .map(position => CardPlayerView(HiddenCard(position)))
      )

    case event: Event.CardsDealtPlayerView => cardsDealtUpdate(event)

    case event: PublicEvent => publicEventUpdate(event)
  }

  val opponents: List[Seat[CardPlayerView]] =
    seats
      .slideUntil(_.playerOption.exists(_.is(me)))
      .filterNot(_.playerOption.exists(_.is(me)))
    
  private def opponent(offset: Int): Option[TakenSeat[CardPlayerView]] =
    extension[T](list: List[T])
      def get(index: Int): Option[T] =
        list match {
          case Nil => None
          case head :: tail if index == 0 => Some(head)
          case head :: tail => tail.get(index - 1)
        }

    opponents.get(offset) collectFirst { case taken: TakenSeat[CardPlayerView] => taken }

  val opponent1: Option[TakenSeat[CardPlayerView]] = opponent(0)
  val opponent2: Option[TakenSeat[CardPlayerView]] = opponent(1)
  val opponent3: Option[TakenSeat[CardPlayerView]] = opponent(2)
  val mainPlayer: Option[TakenSeat[CardPlayerView]] = seatFor(me)
