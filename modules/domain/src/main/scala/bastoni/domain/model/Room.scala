package bastoni.domain.model

import bastoni.domain.model.MatchInfo
import bastoni.domain.model.PlayerState.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

import scala.util.Random

case class BoardCard[C <: CardView](card: C, playedBy: Option[UserId])

object BoardCard:
  given encoder[C <: CardView: Encoder]: Encoder[BoardCard[C]] = deriveEncoder
  given decoder[C <: CardView: Decoder]: Decoder[BoardCard[C]] = deriveDecoder

trait Room[C <: CardView]:
  type RoomView

  val seats: List[Seat[C]]
  val deck: List[C]
  val board: List[BoardCard[C]]
  val matchInfo: Option[MatchInfo]
  val dealerIndex: Option[Int]
  val players: Map[UserId, User]

  val size: Int = seats.size

  def mapOccupiedSeats(f: PartialFunction[PlayerState, OccupiedSeat[C] => OccupiedSeat[C]]): List[Seat[C]] =
    seats.map {
      case seat: OccupiedSeat[C] if f.isDefinedAt(seat.occupant) => f(seat.occupant)(seat)
      case seat                                                  => seat
    }

  def seatFor(userId: UserId): Option[OccupiedSeat[C]] =
    seats.collectFirst { case seat: OccupiedSeat[C] if seat.occupant.is(userId) => seat }

  def seatFor(user: User): Option[OccupiedSeat[C]] = seatFor(user.id)

  val round: List[OccupiedSeat[C]] =
    dealerIndex
      .fold(Nil)(dealer =>
        seats
          .slideUntil(_.index == dealer)
          .collect { case seat: OccupiedSeat[_] => seat }
      )
      .shift

  def dealer: Option[OccupiedSeat[C]]         = round.lastOption
  def nextDealer: Option[OccupiedSeat[C]]     = round.headOption
  def previousDealer: Option[OccupiedSeat[C]] = round.shiftBackwards.lastOption

  protected def buildCard(card: VisibleCard, direction: Direction): C
  protected def faceDown(card: C): C

  protected def updatedPlayers(updatedSeats: List[Seat[C]]): Map[UserId, User] =
    players ++ updatedSeats.collect { case OccupiedSeat(_, player, _, _) => player.id -> User(player.id, player.name) }

  extension [T](list: List[T])
    def removeFirst(cond: T => Boolean): List[T] =
      list match
        case head :: tail if cond(head) => tail
        case head :: tail               => head :: tail.removeFirst(cond)
        case Nil                        => Nil

  protected def removeCard(cards: List[C], card: VisibleCard): List[C] =
    cards.removeFirst(_.card.ref == card.ref)

  protected def updateView(
      seats: List[Seat[C]] = this.seats,
      deck: List[C] = this.deck,
      board: List[BoardCard[C]] = this.board,
      matchInfo: Option[MatchInfo] = this.matchInfo,
      dealerIndex: Option[Int] = this.dealerIndex
  ): RoomView

  def withRequest(command: Command.Act): RoomView = command.action match
    case Action.ShuffleDeck =>
      updateView(
        seats = mapOccupiedSeats {
          case player: Playing if player.is(command.playerId) =>
            _.copy(occupant = player.act(Action.ShuffleDeck, command.timeout))
          case player: Playing => _.copy(occupant = Waiting(player.player))
        },
        matchInfo = matchInfo.map(_.copy(gameScore = None)),
        dealerIndex = seats.collectFirst {
          case seat if seat.playerOption.exists(_.is(command.playerId)) => seat.index
        }
      )

    case action =>
      updateView(
        seats = mapOccupiedSeats {
          case player: Playing if player.is(command.playerId) =>
            _.copy(occupant = player.act(action, command.timeout))
        }
      )

  protected def publicEventUpdate(message: PublicEvent): RoomView =
    message match
      case Event.ClientError(_) =>
        updateView()

      case Event.PlayerJoinedTable(player, targetIndex) =>
        updateView(
          seats = seats.map {
            case seat if seat.index == targetIndex => seat.occupiedBy(SittingOut(player))
            case seat                              => seat
          },
          dealerIndex = dealerIndex.orElse(Some(targetIndex))
        )

      case Event.PlayerLeftTable(player, targetIndex) =>
        updateView(
          seats = seats.map {
            case seat if seat.index == targetIndex => seat.vacant
            case whatever                          => whatever
          },
          dealerIndex = if (dealerIndex.contains(targetIndex)) previousDealer.map(_.index) else dealerIndex
        )

      case Event.MatchStarted(gameType, matchScores) =>
        updateView(
          seats = mapOccupiedSeats {
            case player if matchScores.exists(_.playerIds.contains(player.id)) => _.copy(occupant = player.sitOut.sitIn)
            case player                                                        => _.copy(occupant = player.sitOut)
          },
          matchInfo = Some(MatchInfo(gameType = gameType, matchScore = matchScores, gameScore = None))
        )

      case Event.TrumpRevealed(card) =>
        updateView(
          deck = deck match
            case _ :: tail => tail :+ buildCard(card, Direction.Up)
            case whatever  => whatever
        )

      case Event.BoardCardsDealt(board) =>
        updateView(board = board.map(c => BoardCard(buildCard(c, Direction.Up), playedBy = None)))

      case Event.CardPlayed(playerId, card) =>
        updateView(
          seats = mapOccupiedSeats {
            case acting: Acting if acting.is(playerId) =>
              seat =>
                seat.copy(
                  occupant = acting.done,
                  hand = removeCard(seat.hand, card)
                )
          },
          board = BoardCard(buildCard(card, Direction.Up), playedBy = Some(playerId)) :: board
        )

      case Event.CardsTaken(playerId, taken, scopa) =>
        updateView(
          seats = mapOccupiedSeats {
            case player: Playing if player.is(playerId) =>
              seat =>
                seat.copy(pile =
                  taken.map(card =>
                    buildCard(card, if (scopa.contains(card)) Direction.Up else Direction.Down)
                  ) ++ seat.pile
                )
          },
          board = taken
            .foldLeft(board) { case (remainingBoardCards, toRemove) =>
              remainingBoardCards.filterNot(_.card.ref == toRemove.ref)
            }
            .map(_.copy(playedBy = None)) // who played what isn't important at this point
        )

      case Event.PlayerConfirmed(playerId) =>
        updateView(
          seats = mapOccupiedSeats {
            case player: Acting if player.is(playerId) => _.copy(occupant = player.done)
          }
        )

      case Event.TrickCompleted(winnerId) =>
        updateView(
          seats = mapOccupiedSeats {
            case player if player.is(winnerId) =>
              seat => seat.copy(pile = seat.pile ++ board.map(_.card).map(faceDown))
          },
          board = Nil
        )

      case Event.GameCompleted(scores, matchScores) =>
        extension [T <: Score](score: List[T])
          def pointsFor(player: MatchPlayer): Option[Int] =
            get(player).map(_.points)

          def get(player: MatchPlayer): Option[Score] =
            score.find(_.playerIds.exists(player.is))

        updateView(
          seats = seats.map {
            case seat: OccupiedSeat[C] =>
              seat.copy(
                occupant = seat.occupant match
                  case active: Playing =>
                    EndOfGame(
                      player = active.player
                        .copy(points = matchScores.pointsFor(active.player).getOrElse(active.player.points)),
                      points = scores.pointsFor(active.player).getOrElse(0),
                      winner = scores.bestTeam.exists(active.is)
                    )
                  case whatever => whatever
                ,
                hand = Nil,
                pile = Nil
              )

            case seat: EmptySeat[C] => seat.copy(hand = Nil, pile = Nil)
          },
          deck = Nil,
          board = Nil,
          matchInfo = matchInfo.map(_.copy(matchScore = matchScores, gameScore = Some(scores)))
        )

      case Event.GameAborted(_) =>
        updateView(
          deck = Nil,
          board = Nil,
          seats = seats.map {
            case occupied: OccupiedSeat[C] =>
              occupied.copy(occupant = occupied.occupant.sitOut, hand = Nil, pile = Nil)
            case empty: EmptySeat[C] =>
              empty.copy(hand = Nil, pile = Nil)
          }
        )

      case Event.MatchCompleted(winnerIds) =>
        updateView(
          seats = mapOccupiedSeats { case active: Playing =>
            _.copy(occupant = EndOfMatch(active.player, winner = winnerIds.contains(active.player.id)))
          },
          dealerIndex = nextDealer.map(_.index),
          matchInfo = None
        )

      case Event.MatchAborted(_) =>
        updateView(
          dealerIndex = nextDealer.map(_.index),
          matchInfo = None
        )

      case Event.TimedOut(playerId, _) =>
        updateView(
          seats = mapOccupiedSeats {
            case player: Acting if player.is(playerId) =>
              _.copy(occupant = player.copy(timeout = Some(Timeout.TimedOut)))
          }
        )

  protected def cardsDealtUpdate(event: Event.CardsDealt[C]): RoomView =
    updateView(
      seats = mapOccupiedSeats {
        case player if player.is(event.playerId) =>
          seat => seat.copy(hand = event.cards ++ seat.hand)
      },
      deck = deck.drop(event.cards.size)
    )

  protected def deckShuffledUpdate(newDeck: List[C]): RoomView =
    updateView(
      seats = mapOccupiedSeats { case acting @ Acting(targetPlayer, Action.ShuffleDeck, _) =>
        _.copy(occupant = acting.done)
      },
      deck = newDeck
    )
end Room

object Room:
  given serverRoomViewCodec: Codec[RoomServerView] = deriveCodec
  given playerRoomViewCodec: Codec[RoomPlayerView] = deriveCodec
