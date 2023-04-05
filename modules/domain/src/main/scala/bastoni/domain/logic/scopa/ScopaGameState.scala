package bastoni.domain.logic
package scopa

import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Event.{GameAborted, TimedOut}
import io.circe.{Decoder, Encoder, Json}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

sealed trait ScopaGameState

object ScopaGameState:
  sealed trait Active(val activePlayers: List[MatchPlayer]) extends ScopaGameState
  case class Ready(players: List[MatchPlayer])              extends Active(players)
  case class Deal3Round(todo: List[Player], done: List[Player], deck: Deck)
      extends Active((done ++ todo).map(_.matchPlayer))
  case class Deal5Round(players: List[Player], deck: Deck)         extends Active(players.map(_.matchPlayer))
  case class WillDealBoardCards(players: List[Player], deck: Deck) extends Active(players.map(_.matchPlayer))
  case class WillPlay(round: PlayRound)                            extends Active(round.activePlayers)
  case class DrawRound(
      todo: List[Player],
      done: List[Player],
      deck: Deck,
      boardCards: List[VisibleCard],
      lastTake: Option[UserId]
  ) extends Active((done ++ todo).map(_.matchPlayer))
  case class PlayRound(players: List[Player], deck: Deck, board: List[VisibleCard], lastTake: Option[UserId])
      extends Active(players.map(_.matchPlayer))
  case class WillTakeCards(state: PlayRound, command: Command.TakeCards) extends Active(state.activePlayers)
  case class WillComplete(players: List[Player])                         extends Active(players.map(_.matchPlayer))

  case class WaitingForPlayer(ref: Int, timeout: Timeout.Active, request: Command.Act, state: PlayRound)
      extends Active(state.activePlayers)
      with Timer[ScopaGameState, WaitingForPlayer]:
    override val timedOut: ScopaGameState = ScopaGameState.Aborted(GameAborted.Reason.playerTimeout)
    override def update(timeout: Timeout.Active, request: Command.Act): WaitingForPlayer =
      copy(timeout = timeout, request = request)

  sealed trait Terminated                          extends ScopaGameState
  case class Completed(players: List[MatchPlayer]) extends Terminated
  case class Aborted(reason: GameAborted.Reason)   extends Terminated

  given encoder: Encoder[ScopaGameState] = ConfiguredEncoder.derive(discriminator = Some("stage"))
  given decoder: Decoder[ScopaGameState] = ConfiguredDecoder.derive(discriminator = Some("stage"))

end ScopaGameState
