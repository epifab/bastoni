package bastoni.domain.logic
package briscola

import bastoni.domain.model.*
import io.circe.{ACursor, Encoder, Decoder, DecodingFailure, Json}


case class StateMachine(state: GameState) extends GameStateMachine:
  override def apply(message: Command | Event): (Option[StateMachine], List[Event | Command | Delayed[Command]]) =
    Game.playGameStep(state, message) match
      case (GameState.Terminated, events) => None -> events
      case (state, events) => Some(new StateMachine(state)) -> events

  override val gameType: GameType = GameType.Briscola
  override def encoded: Json = Encoder[GameState].apply(state)


object StateMachine extends GameStateMachineFactory:
  override def apply(room: Room): StateMachine = new StateMachine(GameState(room.players))
  override def decode(json: ACursor): Either[DecodingFailure, StateMachine] = Decoder[GameState].tryDecode(json).map(new StateMachine(_))