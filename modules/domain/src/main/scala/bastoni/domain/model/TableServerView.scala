package bastoni.domain.model

import scala.util.Random

enum TableError:
  case FullTable, PlayerNotFound, DuplicatePlayer

case class TableServerView(
  override val seats: List[Seat[CardServerView]],
  override val deck: List[CardServerView],
  override val active: Boolean
) extends Table[CardServerView]:

  override type TableView = TableServerView

  override protected def updateWith(seats: List[Seat[CardServerView]] = this.seats, deck: List[CardServerView] = this.deck, active: Boolean = this.active): TableServerView =
    TableServerView(seats, deck, active)

  override protected def toC(card: CardServerView): CardServerView = card
  override protected def removeCard(cards: List[CardServerView], card: Card): List[CardServerView] = cards.filterNot(_.card == card)

  def update(event: ServerEvent): TableServerView = event match {
    case Event.DeckShuffledServerPOV(deck) =>
      updateWith(
        seats = seats.map {
          case seat@ Seat(Some(acting@ ActingPlayer(targetPlayer, Action.ShuffleDeck)), _, _, _) =>
            seat.copy(player = Some(acting.done))
          case whatever => whatever
        },
        deck = deck.map(card => CardServerView(card, Face.Down))
      )

    case event: Event.CardDealtServerPOV => cardDealtUpdate(event)

    case event: PublicEvent => publicEventUpdate(event)
  }

  extension (state: CardServerView)
    def toPlayerView(me: Player, player: Option[PlayerState]): CardPlayerView = state match {
      case CardServerView(card, Face.Up) => CardPlayerView(Some(card))
      case CardServerView(card, Face.Down) => CardPlayerView(None)
      case CardServerView(card, Face.Player) => CardPlayerView(Option.when(player.exists(_.playerId == me.id))(card))
    }

  def toPlayerView(me: Player): PlayerTableView =
    PlayerTableView(
      seats = seats.map {
        case Seat(player, hand, collected, played) =>
          Seat[CardPlayerView](
            player = player,
            hand = hand.map(_.toPlayerView(me, player)),
            collected = collected.map(_.toPlayerView(me, player)),
            played = played.map(_.toPlayerView(me, player))
          )
      },
      deck = deck.map(_.toPlayerView(me, None)),
      active = active
    )

  val indexedSeats = seats.zipWithIndex
  val players: List[Player] = seats.collect { case Seat(Some(player), _, _, _) => player.player }
  val size: Int = seats.size
  val isFull: Boolean = seats.forall(_.player.isDefined)
  val isEmpty: Boolean = seats.forall(_.player.isEmpty)

  def contains(p: Player): Boolean = seatIndexFor(p).isDefined

  def seatIndexFor(p: Player): Option[Int] = indexedSeats
    .collectFirst { case (Seat(Some(player), _, _, _), index) if player.playerId == p.id => index }

  def join(p: Player, seed: Int): Either[TableError, TableServerView] =
    if (contains(p)) Left(TableError.DuplicatePlayer) else {
      new Random(seed)
        .shuffle(indexedSeats)
        .collectFirst { case (Seat(None, _, _, _), index) => index }
        .fold[Either[TableError, TableServerView]](Left(TableError.FullTable)) { targetIndex =>
          Right(updateWith(
            seats = indexedSeats.map {
              case (oldSeat, index) if index == targetIndex => oldSeat.copy(player = Some(SittingOut(p)))
              case (seat, _) => seat
            }
          ))
        }
    }

  def leave(p: Player): Either[TableError, TableServerView] =
    seatIndexFor(p) match
      case Some(targetIndex) =>
        Right(copy(
          seats = indexedSeats.map {
            case (oldSeat, index) if index == targetIndex => oldSeat.copy(player = None)
            case (seat, _) => seat
          }
        ))

      case None => Left(TableError.PlayerNotFound)
