package bastoni.domain.model

case class TablePlayerView(
  me: UserId,
  override val seats: List[Seat[CardPlayerView]],
  override val deck: List[CardPlayerView],
  override val board: List[CardPlayerView],
  override val active: Option[GameType]
) extends Table[CardPlayerView]:

  override type TableView = TablePlayerView

  override protected def updateWith(
    seats: List[Seat[CardPlayerView]] = this.seats,
    deck: List[CardPlayerView] = this.deck,
    board: List[CardPlayerView] = this.board,
    active: Option[GameType] = this.active
  ): TablePlayerView = TablePlayerView(me, seats, deck, board, active)

  override protected def buildCard(card: Card, direction: Direction): CardPlayerView = CardPlayerView(direction match {
    case Direction.Up => Some(card)
    case _ => None
  })

  override protected def faceDown(card: CardPlayerView): CardPlayerView = card.copy(card = None)

  extension[T](list: List[T])
    def removeFirst(cond: T => Boolean): List[T] =
      list match {
        case head :: tail if cond(head) => tail
        case head :: tail => head :: tail.removeFirst(cond)
        case Nil => Nil
      }

  override protected def removeCard(cards: List[CardPlayerView], card: Card): List[CardPlayerView] =
    if (cards.exists(_.card.contains(card))) cards.removeFirst(_.card.contains(card))
    else cards.removeFirst(_.card.isEmpty)

  def update(event: PlayerEvent): TablePlayerView = event match {
    case Event.DeckShuffledPlayerView(numberOfCards) => deckShuffledUpdate(List.fill(numberOfCards)(CardPlayerView(None)))

    case event: Event.CardsDealtPlayerView => cardsDealtUpdate(event)

    case event: PublicEvent => publicEventUpdate(event)
  }

  val opponents: List[(Seat[CardPlayerView], Int)] = 
    indexedSeats
      .slideUntil(_._1.player.exists(_.is(me)))
      .filterNot { case (seat, index) => seat.player.exists(_.is(me)) }
    
  def opponent(index: Int): Option[Seat[CardPlayerView]] =
    extension[T](list: List[T])
      def get(index: Int): Option[T] =
        list match {
          case Nil => None
          case head :: tail if index == 0 => Some(head)
          case head :: tail => tail.get(index - 1)
        }
        
    opponents.get(index).map(_._1)

  val mySeat: Option[TakenSeat[CardPlayerView]] = seatFor(me)
