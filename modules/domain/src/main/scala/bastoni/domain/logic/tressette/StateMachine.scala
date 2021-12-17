package bastoni.domain.logic
package tressette

import bastoni.domain.model.*
import io.circe.{ACursor, Encoder, Decoder, DecodingFailure, Json}


case class StateMachine(state: MatchState) extends GameStateMachine:
  override def apply(message: ServerEvent | Command): (Option[StateMachine], List[ServerEvent | Command | Delayed[Command]]) =
    Game.playMatchStep(state, message) match
      case (MatchState.Terminated, events) => None -> events
      case (state, events) => Some(new StateMachine(state)) -> events

  override val gameType: GameType = GameType.Tressette
  override def encoded: Json = Encoder[MatchState].apply(state)


object StateMachine extends GameStateMachineFactory:
  override def apply(table: TableServerView): StateMachine = new StateMachine(MatchState(table.players))
  override def decode(json: ACursor): Either[DecodingFailure, StateMachine] = Decoder[MatchState].tryDecode(json).map(new StateMachine(_))
