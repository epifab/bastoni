package bastoni.domain.logic.tressette

import bastoni.domain.logic.generic.Timer
import bastoni.domain.logic.scopa.ScopaGameState
import bastoni.domain.logic.scopa.ScopaGameState.{Active, PlayRound}
import bastoni.domain.model.*
import bastoni.domain.model.Event.GameAborted
import io.circe.{Decoder, Encoder, Json}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

sealed trait TressetteGameState

object TressetteGameState:
  sealed trait Active(val activePlayers: List[MatchPlayer]) extends TressetteGameState
  case class Ready(players: List[MatchPlayer])              extends Active(players)
  case class DealRound(todo: List[Player], done: List[Player], remaining: Int, deck: Deck)
      extends Active((done ++ todo).map(_.matchPlayer))
  case class DrawRound(todo: List[Player], done: List[Player], deck: Deck)
      extends Active((done ++ todo).map(_.matchPlayer))
  case class PlayRound(todo: List[Player], done: List[(Player, VisibleCard)], deck: Deck)
      extends Active((done.map(_._1) ++ todo).map(_.matchPlayer))
  case class WillPlay(round: PlayRound) extends Active(round.activePlayers)
  case class WillCompleteTrick(players: List[(Player, VisibleCard)], deck: Deck)
      extends Active(players.map(_._1.matchPlayer))
  case class WillComplete(players: List[Player]) extends Active(players.map(_.matchPlayer))

  case class WaitingForPlayer(ref: Int, timeout: Timeout.Active, request: Command.Act, state: PlayRound)
      extends Active(state.activePlayers)
      with Timer[TressetteGameState, WaitingForPlayer]:
    override val timedOut: TressetteGameState = TressetteGameState.Aborted(GameAborted.Reason.playerTimeout)
    override def update(timeout: Timeout.Active, request: Command.Act): WaitingForPlayer =
      copy(timeout = timeout, request = request)

  sealed trait Terminated                          extends TressetteGameState
  case class Completed(players: List[MatchPlayer]) extends Terminated
  case class Aborted(reason: GameAborted.Reason)   extends Terminated

  given encoder: Encoder[TressetteGameState] = ConfiguredEncoder.derive(discriminator = Some("stage"))
  given decoder: Decoder[TressetteGameState] = ConfiguredDecoder.derive(discriminator = Some("stage"))
