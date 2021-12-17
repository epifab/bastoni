package bastoni.domain.logic
package scopa

import bastoni.domain.model.{GameType, TableServerView}
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, Json}

case class StateMachine(state: GameState) extends GameStateMachine:
  override def apply(message: StateMachineInput): (Option[StateMachine], List[StateMachineOutput]) =
    Game.playGameStep(state, message) match
      case (GameState.Terminated, events) => None -> events
      case (state, events) => Some(new StateMachine(state)) -> events

  override val gameType: GameType = GameType.Briscola
  override def encoded: Json = Encoder[GameState].apply(state)


object StateMachine extends GameStateMachineFactory:
  override def apply(table: TableServerView): StateMachine = new StateMachine(GameState(table.players))
  override def decode(json: ACursor): Either[DecodingFailure, StateMachine] = Decoder[GameState].tryDecode(json).map(new StateMachine(_))
