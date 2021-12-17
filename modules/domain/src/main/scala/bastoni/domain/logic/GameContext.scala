package bastoni.domain.logic

import bastoni.domain.logic.GameStateMachine
import bastoni.domain.model.*
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.{Codec, Decoder, Encoder}

import scala.util.Random

case class GameContext(table: Table, stateMachine: Option[GameStateMachine]):
  def withStateMachine(stateMachine: Option[GameStateMachine]) = copy(stateMachine = stateMachine)
  def update(message: Event | Command): GameContext = copy(table = table.update(message))


object GameContext:
  given Codec[Seat] = deriveCodec
  given Codec[GameContext] = deriveCodec

  def build(data: Event | Command): Option[GameContext] =
    val room = data match {
      case event: Event.RoomEvent => Some(event.room)
      case command: Command.StartGame => Some(command.room)
      case _ => None
    }

    room.map { room =>
      new GameContext(
        Table(
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
        ),
        stateMachine = None,
      )
    }
