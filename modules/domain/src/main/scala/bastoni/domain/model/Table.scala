package bastoni.domain.model

import bastoni.domain.model.MatchInfo
import bastoni.domain.model.PlayerState.*
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.util.Random

case class Seat[C <: CardView](
  index: Int,
  player: Option[PlayerState],
  hand: List[C],
  taken: List[C]
)

object Seat:
  given playerView: Codec[Seat[CardPlayerView]] = deriveCodec
  given serverView: Codec[Seat[CardServerView]] = deriveCodec

case class TakenSeat[C <: CardView](player: PlayerState, hand: List[C], taken: List[C], dealer: Boolean)

trait Table[C <: CardView]:
  type TableView

  def seats: List[Seat[C]]
  def deck: List[C]
  def board: List[(Option[UserId], C)]
  def matchInfo: Option[MatchInfo]
  def dealerIndex: Option[Int]

  def modSeats(f: PartialFunction[Option[PlayerState], Seat[C] => Seat[C]]): List[Seat[C]] =
    seats.map {
      case seat if f.isDefinedAt(seat.player) => f(seat.player)(seat)
      case seat => seat
    }

  def activePlayers: List[UserId] = matchInfo.fold(Nil)(_.matchScore.flatMap(_.playerIds))

  def dealer: Option[Seat[C]] = dealerIndex.flatMap(dealerIndex => seats.find(_.index == dealerIndex))

  protected def buildCard(card: VisibleCard, direction: Direction): C
  protected def faceDown(card: C): C

  extension[T](list: List[T])
    def removeFirst(cond: T => Boolean): List[T] =
      list match {
        case head :: tail if cond(head) => tail
        case head :: tail => head :: tail.removeFirst(cond)
        case Nil => Nil
      }

  protected def removeCard(cards: List[C], card: VisibleCard): List[C] =
    cards.removeFirst(_.card.ref == card.ref)

  protected def updateWith(
    seats: List[Seat[C]] = this.seats,
    deck: List[C] = this.deck,
    board: List[(Option[UserId], C)] = this.board,
    matchInfo: Option[MatchInfo] = matchInfo,
    dealerIndex: Option[Int] = dealerIndex
  ): TableView

  protected def publicEventUpdate(message: PublicEvent): TableView =
    message match
      case Event.PlayerJoinedTable(player, targetIndex) =>
        updateWith(
          seats = seats.map {
            case seat if seat.index == targetIndex => seat.copy(player = Some(SittingOut(player)))
            case seat => seat
          },
          dealerIndex = dealerIndex.orElse(Some(targetIndex))
        )

      case Event.PlayerLeftTable(player, targetIndex) =>
        updateWith(
          seats = seats.map {
            case seat if seat.index == targetIndex => seat.copy(player = None)
            case whatever => whatever
          }
        )

      case Event.MatchStarted(gameType, matchScores) =>
        updateWith(
          seats = modSeats {
            case Some(player) if matchScores.exists(_.playerIds.contains(player.id)) => _.copy(player = Some(player.sitIn))
            case Some(player) => _.copy(player = Some(player.sitOut))
          },
          matchInfo = Some(MatchInfo(gameType, matchScores, None))
        )

      case Event.TrumpRevealed(card) =>
        updateWith(
          deck = deck match {
            case head :: tail => tail :+ buildCard(card, Direction.Up)
            case whatever => whatever
          }
        )

      case Event.BoardCardsDealt(board) =>
        updateWith(board = board.map(c => None -> buildCard(c, Direction.Up)))

      case Event.CardPlayed(playerId, card) =>
        updateWith(
          seats = modSeats {
            case Some(acting: ActingPlayer) if acting.is(playerId) =>
              seat => seat.copy(
                player = Some(acting.done),
                hand = removeCard(seat.hand, card)
              )
          },
          board = Some(playerId) -> buildCard(card, Direction.Up) :: board
        )

      case Event.CardsTaken(playerId, taken, scopa) =>
        updateWith(
          seats = modSeats {
            case Some(player: SittingIn) if player.is(playerId) =>
              seat => seat.copy(
                taken = taken.map(card => buildCard(card, if (scopa.contains(card)) Direction.Up else Direction.Down)) ++ seat.taken
              )
          },
          board = taken.foldLeft(board) {
            case (remainingBoardCards, toRemove) => remainingBoardCards.filterNot {
              case (_, boardCard) => boardCard.card.ref == toRemove.ref
            }
          }
        )

      case Event.TrickCompleted(winnerId) =>
        updateWith(
          seats = seats.map {
            case seat if seat.player.exists(_.is(winnerId)) => seat.copy(taken = seat.taken ++ board.map { case (_, card) => faceDown(card) })
            case whatever => whatever
          },
          board = Nil
        )

      case Event.GameCompleted(scores, matchScores) =>
        extension[T <: Score] (score: List[T])
          def pointsFor(player: MatchPlayer): Option[Int] =
            get(player).map(_.points)

          def get(player: MatchPlayer): Option[Score] =
            score.find(_.playerIds.exists(player.is))

        updateWith(
          seats = seats.map(seat => seat.copy(
            player = seat.player.map {
              case active: SittingIn =>
                EndOfGamePlayer(
                  player = active.player.copy(points = matchScores.pointsFor(active.player).getOrElse(active.player.points)),
                  points = scores.pointsFor(active.player).getOrElse(0),
                  winner = scores.bestTeam.exists(active.is)
                )
              case whatever => whatever
            },
            hand = Nil,
            taken = Nil
          )),
          deck = Nil,
          board = Nil,
          matchInfo = matchInfo.map(_.copy(matchScore = matchScores, gameScore = Some(scores)))
        )

      case Event.MatchCompleted(winnerIds) =>
        updateWith(
          seats = modSeats {
            case Some(active: SittingIn) =>
              _.copy(player = Some(EndOfMatchPlayer(active.player, winner = winnerIds.contains(active.player.id))))
          },
          matchInfo = None
        )

      case Event.GameAborted | Event.MatchAborted =>
        updateWith(
          seats = modSeats {
            case Some(player: SittingIn) =>
              _.copy(player = Some(player.sitOut), hand = Nil, taken = Nil)
            case _ =>
              _.copy(hand = Nil, taken = Nil)
          },
          deck = Nil,
          board = Nil,
          matchInfo = None
        )

      case Event.ActionRequested(playerId, Action.ShuffleDeck, timeout) =>
        updateWith(
          seats = modSeats {
            case Some(player) if player.is(playerId) => _.copy(player = Some(player.sitIn.act(Action.ShuffleDeck, timeout)))
            case Some(player) if activePlayers.contains(player.id) => _.copy(player = Some(player.sitIn))
            case Some(player) => _.copy(player = Some(player.sitOut))
          },
          matchInfo = matchInfo.map(_.copy(gameScore = None)),
          dealerIndex = seats.collectFirst { case seat if seat.player.exists(_.is(playerId)) => seat.index }
        )

      case Event.ActionRequested(playerId, action, timeout) =>
        updateWith(
          seats = modSeats {
            case Some(player: SittingIn) if player.is(playerId) =>
              _.copy(player = Some(player.act(action, timeout)))
          }
        )

      case Event.TimedOut(playerId, _) =>
        updateWith(
          seats = modSeats {
            case Some(player: ActingPlayer) if player.is(playerId) =>
              _.copy(player = Some(player.copy(timeout = Some(Timeout.TimedOut))))
          }
        )

  protected def cardsDealtUpdate(event: Event.CardsDealt[C]): TableView =
    updateWith(
      seats = seats.map {
        case seat if seat.player.exists(_.is(event.playerId)) =>
          seat.copy(hand = event.cards ++ seat.hand)
        case whatever => whatever
      },
      deck = deck.drop(event.cards.size)
    )

  protected def deckShuffledUpdate(newDeck: List[C]): TableView =
    updateWith(
      seats = modSeats {
        case Some(acting@ ActingPlayer(targetPlayer, Action.ShuffleDeck, _)) =>
          _.copy(player = Some(acting.done))
      },
      deck = newDeck
    )

  def seatFor(userId: UserId): Option[TakenSeat[C]] =
    seats.collectFirst {
      case Seat(index, Some(player), hand, taken) if player.is(userId) =>
        TakenSeat(player, hand, taken, dealer = dealerIndex.contains(index))
    }

  def seatFor(user: User): Option[TakenSeat[C]] = seatFor(user.id)


object Table:
  given serverTableViewCodec: Codec[TableServerView] = deriveCodec
  given playerTableViewCodec: Codec[TablePlayerView] = deriveCodec
