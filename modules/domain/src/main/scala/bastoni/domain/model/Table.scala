package bastoni.domain.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class Seat[C <: CardView](
  player: Option[PlayerState],
  hand: List[C],
  collected: List[C],
  played: List[C]
)

object Seat:
  given playerView: Codec[Seat[PlayerCardView]] = deriveCodec
  given serverView: Codec[Seat[ServerCardView]] = deriveCodec

case class PlayerSeat[C <: CardView](player: PlayerState, hand: List[C], collected: List[C], played: List[C])

sealed trait Table[C <: CardView]:
  type TableView

  def seats: List[Seat[C]]
  def deck: List[C]
  def active: Boolean

  protected def toC(card: ServerCardView): C
  protected def removeCard(cards: List[C], card: Card): List[C]

  protected def updateWith(seats: List[Seat[C]] = this.seats, deck: List[C] = this.deck, active: Boolean = active): TableView

  protected def publicEventUpdate(message: PublicEvent): TableView =
    message match
      case Event.PlayerJoined(player, room) =>
        updateWith(
          seats = seats.zip(room.seats).map {
            case (seat, Some(targetPlayer)) if targetPlayer.id == player.id => seat.copy(Some(SittingOut(player)))
            case (whatever, _) => whatever
          }
        )

      case Event.PlayerLeft(player, room) =>
        updateWith(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == player.id) => seat.copy(player = None)
            case whatever => whatever
          }
        )

      case Event.GameStarted(_) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(sittingOut: SittingOut), _, _, _) => seat.copy(player = Some(sittingOut.sitIn))
            case whatever => whatever
          },
          active = true
        )

      case Event.TrumpRevealed(card) =>
        updateWith(
          deck = deck match {
            case head :: tail => tail :+ toC(ServerCardView(card, Face.Up))
            case whatever => whatever
          }
        )

      case Event.CardPlayed(playerId, card) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(acting: ActingPlayer), _, _, _) if acting.playerId == playerId =>
              seat.copy(
                player = Some(acting.done),
                hand = removeCard(seat.hand, card),
                played = toC(ServerCardView(card, Face.Up)) :: seat.played
              )
            case whatever => whatever
          }
        )

      case Event.TrickCompleted(winnerId) =>
        updateWith(
          seats = seats.map {
            case seat if seat.player.exists(_.playerId == winnerId) =>
              seat.copy(
                collected = seat.collected ++ seats.flatMap(_.played),
                played = Nil
              )
            case seat => seat.copy(played = Nil)
          }
        )

      case Event.MatchCompleted(winnerIds, matchPoints, gamePoints) =>
        extension (points: List[PointsCount])
          def pointsFor(player: GamePlayer): Option[Int] =
            points.find(_.playerIds.exists(player.is)).map(_.points)

        updateWith(
          seats = seats.map(seat => seat.copy(
            player = seat.player.map {
              case active: SittingIn =>
                EndOfMatchPlayer(
                  player = active.player.copy(points = gamePoints.pointsFor(active.player).getOrElse(active.player.points)),
                  points = matchPoints.pointsFor(active.player).getOrElse(0),
                  winner = winnerIds.exists(active.player.is)
                )
              case whatever => whatever
            },
            hand = Nil,
            collected = Nil,
            played = Nil
          )),
          deck = Nil
        )

      case Event.GameCompleted(winnerIds) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(active: SittingIn), _, _, _) =>
              seat.copy(player = Some(EndOfGamePlayer(active.player, winner = winnerIds.contains(active.player.id))))
            case whatever => whatever
          },
          deck = Nil,
          active = false
        )

      case Event.MatchAborted | Event.GameAborted =>
        updateWith(
          seats = seats.map {
            case Seat(Some(player: SittingIn), _, _, _) =>
              Seat(Some(player.sitOut), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil
        )

      case Event.ActionRequested(playerId, Action.ShuffleDeck) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(waiting: SittingIn), _, _, _) if waiting.playerId == playerId =>
              seat.copy(player = Some(waiting.act(Action.ShuffleDeck).mapPlayer(_.copy(dealer = true))))
            case seat@ Seat(Some(sittingIn: SittingIn), _, _, _) =>
              seat.copy(player = Some(sittingIn.mapPlayer(_.copy(dealer = false))))
            case whatever => whatever
          }
        )

      case Event.ActionRequested(playerId, action) =>
        updateWith(
          seats = seats.map {
            case seat@ Seat(Some(waiting: SittingIn), _, _, _) if waiting.playerId == playerId =>
              seat.copy(player = Some(waiting.act(action)))
            case whatever => whatever
          }
        )

  protected def cardDealtUpdate(event: Event.CardDealt[C]): TableView =
    updateWith(
      seats = seats.map {
        case seat if seat.player.exists(_.playerId == event.playerId) =>
          seat.copy(hand = event.card :: seat.hand)
        case whatever => whatever
      },
      deck = deck match {
        case head :: tail => tail
        case whatever => whatever
      }
    )

  def seatFor(player: Player): Option[PlayerSeat[C]] =
    seats.collectFirst {
      case Seat(Some(p), hand, collected, played) if p.playerId == player.id =>
        PlayerSeat(p, hand, collected, played)
    }


case class ServerTableView(
  override val seats: List[Seat[ServerCardView]],
  override val deck: List[ServerCardView],
  override val active: Boolean
) extends Table[ServerCardView]:

  override type TableView = ServerTableView

  override protected def updateWith(seats: List[Seat[ServerCardView]] = this.seats, deck: List[ServerCardView] = this.deck, active: Boolean = this.active): ServerTableView =
    ServerTableView(seats, deck, active)

  override protected def toC(card: ServerCardView): ServerCardView = card
  override protected def removeCard(cards: List[ServerCardView], card: Card): List[ServerCardView] = cards.filterNot(_.card == card)

  def update(event: ServerEvent): ServerTableView = event match {
    case Event.DeckShuffledServerPOV(deck) =>
      updateWith(
        seats = seats.map {
          case seat@ Seat(Some(acting@ ActingPlayer(targetPlayer, Action.ShuffleDeck)), _, _, _) =>
            seat.copy(player = Some(acting.done))
          case whatever => whatever
        },
        deck = deck.map(card => ServerCardView(card, Face.Down))
      )

    case event: Event.CardDealtServerPOV => cardDealtUpdate(event)

    case event: PublicEvent => publicEventUpdate(event)
  }

  extension (state: ServerCardView)
    def toPlayerView(me: Player, player: Option[PlayerState]): PlayerCardView = state match {
      case ServerCardView(card, Face.Up) => PlayerCardView(Some(card))
      case ServerCardView(card, Face.Down) => PlayerCardView(None)
      case ServerCardView(card, Face.Player) => PlayerCardView(Option.when(player.exists(_.playerId == me.id))(card))
    }

  def toPlayerView(me: Player): PlayerTableView =
    PlayerTableView(
      seats = seats.map {
        case Seat(player, hand, collected, played) =>
          Seat[PlayerCardView](
            player = player,
            hand = hand.map(_.toPlayerView(me, player)),
            collected = collected.map(_.toPlayerView(me, player)),
            played = played.map(_.toPlayerView(me, player))
          )
      },
      deck = deck.map(_.toPlayerView(me, None)),
      active = active
    )


case class PlayerTableView(
  override val seats: List[Seat[PlayerCardView]],
  override val deck: List[PlayerCardView],
  override val active: Boolean
) extends Table[PlayerCardView]:

  override type TableView = PlayerTableView

  override protected def updateWith(seats: List[Seat[PlayerCardView]] = this.seats, deck: List[PlayerCardView] = this.deck, active: Boolean = this.active): PlayerTableView =
    PlayerTableView(seats, deck, active)

  override protected def toC(card: ServerCardView): PlayerCardView = PlayerCardView(card.face match {
    case Face.Up => Some(card.card)
    case _ => None
  })

  extension[T](list: List[T])
    def removeFirst(cond: T => Boolean): List[T] =
      list match {
        case head :: tail if cond(head) => tail
        case head :: tail => head :: tail.removeFirst(cond)
        case Nil => Nil
      }

  override protected def removeCard(cards: List[PlayerCardView], card: Card): List[PlayerCardView] =
    if (cards.exists(_.card.contains(card))) cards.removeFirst(_.card.contains(card))
    else cards.removeFirst(_.card.isEmpty)

  def update(event: PlayerEvent): PlayerTableView = event match {
    case Event.DeckShuffledPlayerPOV(numberOfCards) =>
      updateWith(
        seats = seats.map {
          case seat@Seat(Some(acting@ActingPlayer(targetPlayer, Action.ShuffleDeck)), _, _, _) =>
            seat.copy(player = Some(acting.done))
          case whatever => whatever
        },
        deck = List.fill(numberOfCards)(PlayerCardView(None))
      )

    case event: Event.CardDealtPlayerPOV => cardDealtUpdate(event)

    case event: PublicEvent => publicEventUpdate(event)
  }


object Table:
  given serverTableViewCodec: Codec[ServerTableView] = deriveCodec
  given playerTableViewCodec: Codec[PlayerTableView] = deriveCodec

  def apply(message: ServerEvent): Option[ServerTableView] =
    val room = message match {
      case event: Event.RoomEvent => Some(event.room)
      case _ => None
    }

    room.map { room =>
      ServerTableView(
        seats = room.seats.map(seat =>
          Seat(
            seat.map(SittingOut(_)),
            hand = Nil,
            collected = Nil,
            played = Nil
          )
        ),
        deck = Nil,
        active = false
      )
    }
