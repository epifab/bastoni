package bastoni.domain.model

import bastoni.domain.logic.GameStateMachine
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

import scala.util.Random

case class Seat(
  player: Option[PlayerState],
  hand: List[CardState],
  collected: List[CardState],
  played: List[CardState]
)

case class GameRoom(seats: List[Seat], deck: List[CardState], stateMachine: Option[GameStateMachine]):

  def withStateMachine(stateMachine: Option[GameStateMachine]) = copy(stateMachine = stateMachine)

  def update(event: Event | Command): GameRoom = {
    event match
      case Event.PlayerJoined(player, room) =>
        copy(
          seats = seats.zip(room.seats).map {
            case (snapshotSeat, Some(targetPlayer)) if targetPlayer.id == player.id => snapshotSeat.copy(Some(SittingOut(player)))
            case (snapshotSeat, _) => snapshotSeat
          }
        )

      case Event.PlayerLeft(player, room) =>
        copy(
          seats = seats.zip(room.seats).map {
            case (Seat(Some(p), hand, collected, played), _) if p.playerId == player.id => Seat(None, hand, collected, played)
            case (snapshotSeat, _) => snapshotSeat
          }
        )

      case Event.GameStarted(_) => this

      case Event.DeckShuffled(deck) =>
        copy(
          seats = seats.map {
            case (Seat(Some(SittingOut(p)), _, _, _)) => Seat(Some(ActivePlayer(GamePlayer(p, 0))), Nil, Nil, Nil)
            case (Seat(Some(p: SittingIn), _, _, _)) => Seat(Some(ActivePlayer(p.player)), Nil, Nil, Nil)
            case whatever => whatever
          },
          deck = deck.map(card => CardState(card, Face.Down))
        )

      case Event.TrumpRevealed(card) =>
        copy(
          deck = deck match {
            case head :: tail if head.card == card => tail :+ CardState(card, Face.Up)
            case whatever => whatever
          }
        )

      case Event.CardDealt(playerId, card, face) =>
        copy(
          seats = seats.map {
            case Seat(Some(p), hand, collected, played) if p.playerId == playerId && deck.headOption.exists(_.card == card) =>
              Seat(Some(p), CardState(card, face) :: hand, collected, played)
            case Seat(Some(p), hand, collected, played) if p.playerId == playerId =>
              if (!deck.headOption.exists(_.card == card)) throw new Exception(s"Card: $card Deck: ${deck.map(_.card).mkString(", ")}")
              else Seat(Some(p), CardState(card, face) :: hand, collected, played)
            case whatever => whatever
          },
          deck = deck match {
            case head :: tail if head.card == card => tail
            case whatever => whatever
          }
        )

      case Event.CardPlayed(playerId, card) =>
        copy(
          seats = seats.map {
            case Seat(Some(ActingPlayer(player)), hand, collected, played) if player.id == playerId && hand.exists(_.card == card) =>
              Seat(Some(ActivePlayer(player)), hand.filterNot(_.card == card), collected, CardState(card, Face.Up) :: played)
            case whatever => whatever
          }
        )

      case Event.TrickCompleted(winnerId) =>
        copy(
          seats = seats.map {
            case Seat(Some(p), hand, collected, played) if p.playerId == winnerId =>
              Seat(Some(p), hand, collected ++ seats.flatMap(_.played), Nil)
            case Seat(p, hand, collected, _) =>
              Seat(p, hand, collected, Nil)
          }
        )

      case Event.MatchPointsCount(playerIds, points) =>
        copy(
          seats = seats.map {
            case Seat(Some(active: SittingIn), hand, collected, played) if playerIds.contains(active.player.id) =>
              Seat(Some(PlayerWithPoints(active.player, points)), hand, collected, played)
            case whatever => whatever
          }
        )

      case Event.GamePointsCount(playerIds, points) =>
        copy(
          seats = seats.map {
            case Seat(Some(done: EndOfMatchPlayer), hand, collected, played) if playerIds.contains(done.player.id) =>
              Seat(Some(done.copy(player = done.player.copy(points = points))), hand, collected, played)
            case whatever => whatever
          }
        )

      case Event.MatchCompleted(winnerIds) =>
        copy(
          seats = seats.map {
            case Seat(Some(PlayerWithPoints(player, points)), _, _, _) =>
              Seat(Some(EndOfMatchPlayer(player, points, winner = winnerIds.contains(player.id))), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil
        )

      case Event.GameCompleted(winnerIds) =>
        copy(
          seats = seats.map {
            case Seat(Some(active: SittingIn), _, _, _) =>
              Seat(Some(EndOfGamePlayer(active.player, winner = winnerIds.contains(active.player.id))), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil
        )

      case Event.MatchDraw =>
        copy(
          seats = seats.map {
            case Seat(Some(PlayerWithPoints(p, points)), _, _, _) =>
              Seat(Some(EndOfMatchPlayer(p, points, winner = false)), Nil, Nil, Nil)
            case whatever => whatever
          },
          deck = Nil
        )

      case Event.MatchAborted | Event.GameAborted =>
        copy(
          seats = seats.map {
            case Seat(Some(p: SittingIn), _, _, _) =>
              Seat(Some(SittingOut(p.player.player)), Nil, Nil, Nil)
            case Seat(whatever, _, _, _) =>
              Seat(whatever, Nil, Nil, Nil)
          },
          deck = Nil
        )

      case Command.ActionRequest(playerId, _) =>
        copy(
          seats = seats.map {
            case Seat(Some(ActivePlayer(player)), hand, collected, played) if player.id == playerId =>
              Seat(Some(ActingPlayer(player)), hand, collected, played)
            case whatever => whatever
          }
        )

      case _: Command => this
  }


object GameRoom:
  given Codec[Seat] = deriveCodec
  given Codec[GameRoom] = deriveCodec

  def build(data: Event | Command): Option[GameRoom] =
    val room = data match {
      case event: Event.RoomEvent => Some(event.room)
      case command: Command.StartGame => Some(command.room)
      case _ => None
    }

    room.map { room =>
      new GameRoom(
        seats = room.seats.map(seat =>
          Seat(
            seat.map(SittingOut(_)),
            hand = Nil,
            collected = Nil,
            played = Nil
          )
        ),
        deck = Nil,
        stateMachine = None
      )
    }
