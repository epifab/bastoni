package bastoni.domain.model

import bastoni.domain.model.MatchInfo
import bastoni.domain.model.PlayerState.*
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

import scala.util.Random

case class BoardCard[C <: CardView](card: C, playedBy: Option[UserId])

object BoardCard:
  given codec[C <: CardView: Codec]: Codec[BoardCard[C]] = deriveCodec

trait Room[C <: CardView]:
  type RoomView

  val seats: List[Seat[C]]
  val deck: List[C]
  val board: List[BoardCard[C]]
  val matchInfo: Option[MatchInfo]
  val dealerIndex: Option[Int]
  val players: Map[UserId, User]

  val size: Int = seats.size

  def mapTakenSeats(f: PartialFunction[PlayerState, TakenSeat[C] => TakenSeat[C]]): List[Seat[C]] =
    seats.map {
      case seat: TakenSeat[C] if f.isDefinedAt(seat.player) => f(seat.player)(seat)
      case seat                                             => seat
    }

  def seatFor(userId: UserId): Option[TakenSeat[C]] =
    seats.collectFirst { case seat: TakenSeat[C] if seat.player.is(userId) => seat }

  def seatFor(user: User): Option[TakenSeat[C]] = seatFor(user.id)

  val round: List[TakenSeat[C]] =
    dealerIndex
      .fold(Nil)(dealer =>
        seats
          .slideUntil(_.index == dealer)
          .collect { case seat: TakenSeat[_] => seat }
      )
      .shift

  def dealer: Option[TakenSeat[C]]         = round.lastOption
  def nextDealer: Option[TakenSeat[C]]     = round.headOption
  def previousDealer: Option[TakenSeat[C]] = round.shiftBackwards.lastOption

  protected def buildCard(card: VisibleCard, direction: Direction): C
  protected def faceDown(card: C): C

  protected def updatedPlayers(updatedSeats: List[Seat[C]]): Map[UserId, User] =
    players ++ updatedSeats.collect { case TakenSeat(_, player, _, _) => player.id -> User(player.id, player.name) }

  extension [T](list: List[T])
    def removeFirst(cond: T => Boolean): List[T] =
      list match
        case head :: tail if cond(head) => tail
        case head :: tail               => head :: tail.removeFirst(cond)
        case Nil                        => Nil

  protected def removeCard(cards: List[C], card: VisibleCard): List[C] =
    cards.removeFirst(_.card.ref == card.ref)

  protected def updateWith(
      seats: List[Seat[C]] = this.seats,
      deck: List[C] = this.deck,
      board: List[BoardCard[C]] = this.board,
      matchInfo: Option[MatchInfo] = matchInfo,
      dealerIndex: Option[Int] = dealerIndex
  ): RoomView

  def withRequest(command: Command.Act): RoomView = command.action match
    case Action.ShuffleDeck =>
      updateWith(
        seats = mapTakenSeats {
          case player: SittingIn if player.is(command.playerId) =>
            _.copy(player = player.act(Action.ShuffleDeck, command.timeout))
          case player: SittingIn => _.copy(player = WaitingPlayer(player.player))
        },
        matchInfo = matchInfo.map(_.copy(gameScore = None)),
        dealerIndex = seats.collectFirst { case seat if seat.playerOption.exists(_.is(command.playerId)) => seat.index }
      )

    case action =>
      updateWith(
        seats = mapTakenSeats {
          case player: SittingIn if player.is(command.playerId) =>
            _.copy(player = player.act(action, command.timeout))
        }
      )

  protected def publicEventUpdate(message: PublicEvent): RoomView =
    message match
      case Event.PlayerJoinedRoom(player, targetIndex) =>
        updateWith(
          seats = seats.map {
            case seat if seat.index == targetIndex => seat.occupiedBy(SittingOut(player))
            case seat                              => seat
          },
          dealerIndex = dealerIndex.orElse(Some(targetIndex))
        )

      case Event.PlayerLeftRoom(player, targetIndex) =>
        updateWith(
          seats = seats.map {
            case seat if seat.index == targetIndex => seat.vacant
            case whatever                          => whatever
          },
          dealerIndex = if (dealerIndex.contains(targetIndex)) previousDealer.map(_.index) else dealerIndex
        )

      case Event.MatchStarted(gameType, matchScores) =>
        updateWith(
          seats = mapTakenSeats {
            case player if matchScores.exists(_.playerIds.contains(player.id)) => _.copy(player = player.sitOut.sitIn)
            case player                                                        => _.copy(player = player.sitOut)
          },
          matchInfo = Some(MatchInfo(gameType, matchScores, None))
        )

      case Event.TrumpRevealed(card) =>
        updateWith(
          deck = deck match
            case head :: tail => tail :+ buildCard(card, Direction.Up)
            case whatever     => whatever
        )

      case Event.BoardCardsDealt(board) =>
        updateWith(board = board.map(c => BoardCard(buildCard(c, Direction.Up), playedBy = None)))

      case Event.CardPlayed(playerId, card) =>
        updateWith(
          seats = mapTakenSeats {
            case acting: ActingPlayer if acting.is(playerId) =>
              seat =>
                seat.copy(
                  player = acting.done,
                  hand = removeCard(seat.hand, card)
                )
          },
          board = BoardCard(buildCard(card, Direction.Up), playedBy = Some(playerId)) :: board
        )

      case Event.CardsTaken(playerId, taken, scopa) =>
        updateWith(
          seats = mapTakenSeats {
            case player: SittingIn if player.is(playerId) =>
              seat =>
                seat.copy(taken =
                  taken.map(card =>
                    buildCard(card, if (scopa.contains(card)) Direction.Up else Direction.Down)
                  ) ++ seat.taken
                )
          },
          board = taken
            .foldLeft(board) { case (remainingBoardCards, toRemove) =>
              remainingBoardCards.filterNot(_.card.ref == toRemove.ref)
            }
            .map(_.copy(playedBy = None)) // who played what isn't important at this point
        )

      case Event.PlayerConfirmed(playerId) =>
        updateWith(
          seats = mapTakenSeats {
            case player: ActingPlayer if player.is(playerId) => _.copy(player = player.done)
          }
        )

      case Event.TrickCompleted(winnerId) =>
        updateWith(
          seats = mapTakenSeats {
            case player if player.is(winnerId) =>
              seat => seat.copy(taken = seat.taken ++ board.map(_.card).map(faceDown))
          },
          board = Nil
        )

      case Event.GameCompleted(scores, matchScores) =>
        extension [T <: Score](score: List[T])
          def pointsFor(player: MatchPlayer): Option[Int] =
            get(player).map(_.points)

          def get(player: MatchPlayer): Option[Score] =
            score.find(_.playerIds.exists(player.is))

        updateWith(
          seats = seats.map {
            case seat: TakenSeat[C] =>
              seat.copy(
                player = seat.player match
                  case active: SittingIn =>
                    EndOfGamePlayer(
                      player = active.player
                        .copy(points = matchScores.pointsFor(active.player).getOrElse(active.player.points)),
                      points = scores.pointsFor(active.player).getOrElse(0),
                      winner = scores.bestTeam.exists(active.is)
                    )
                  case whatever => whatever
                ,
                hand = Nil,
                taken = Nil
              )

            case seat: EmptySeat[C] => seat.copy(hand = Nil, taken = Nil)
          },
          deck = Nil,
          board = Nil,
          matchInfo = matchInfo.map(_.copy(matchScore = matchScores, gameScore = Some(scores)))
        )

      case Event.GameAborted(_) =>
        updateWith(
          deck = Nil,
          board = Nil,
          seats = seats.map {
            case taken: TakenSeat[C] =>
              taken.copy(player = taken.player.sitOut, hand = Nil, taken = Nil)
            case empty: EmptySeat[C] =>
              empty.copy(hand = Nil, taken = Nil)
          }
        )

      case Event.MatchCompleted(winnerIds) =>
        updateWith(
          seats = mapTakenSeats { case active: SittingIn =>
            _.copy(player = EndOfMatchPlayer(active.player, winner = winnerIds.contains(active.player.id)))
          },
          dealerIndex = nextDealer.map(_.index),
          matchInfo = None
        )

      case Event.MatchAborted(_) =>
        updateWith(
          dealerIndex = nextDealer.map(_.index),
          matchInfo = None
        )

      case Event.TimedOut(playerId, _) =>
        updateWith(
          seats = mapTakenSeats {
            case player: ActingPlayer if player.is(playerId) =>
              _.copy(player = player.copy(timeout = Some(Timeout.TimedOut)))
          }
        )

  protected def cardsDealtUpdate(event: Event.CardsDealt[C]): RoomView =
    updateWith(
      seats = mapTakenSeats {
        case player if player.is(event.playerId) =>
          seat => seat.copy(hand = event.cards ++ seat.hand)
      },
      deck = deck.drop(event.cards.size)
    )

  protected def deckShuffledUpdate(newDeck: List[C]): RoomView =
    updateWith(
      seats = mapTakenSeats { case acting @ ActingPlayer(targetPlayer, Action.ShuffleDeck, _) =>
        _.copy(player = acting.done)
      },
      deck = newDeck
    )
end Room

object Room:
  given serverRoomViewCodec: Codec[RoomServerView] = deriveCodec
  given playerRoomViewCodec: Codec[RoomPlayerView] = deriveCodec
