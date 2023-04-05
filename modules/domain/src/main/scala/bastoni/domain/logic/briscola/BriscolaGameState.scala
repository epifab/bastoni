package bastoni.domain.logic.briscola

import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Event.GameAborted
import io.circe.{Decoder, Encoder, Json}
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps

sealed trait BriscolaGameState

object BriscolaGameState:
  sealed trait Active(val activePlayers: List[MatchPlayer]) extends BriscolaGameState

  case class Ready(players: List[MatchPlayer]) extends Active(players)

  case class DealRound(todo: List[Player], done: List[Player], deck: Deck)
      extends Active((done ++ todo).map(_.matchPlayer))

  case class WillDealTrump(players: List[Player], deck: Deck) extends Active(players.map(_.matchPlayer))

  case class WillPlay(round: PlayRound) extends Active(round.activePlayers)

  case class DrawRound(todo: List[Player], done: List[Player], deck: Deck, trump: VisibleCard)
      extends Active((done ++ todo).map(_.matchPlayer))

  case class PlayRound(todo: List[Player], done: List[(Player, VisibleCard)], deck: Deck, trump: VisibleCard)
      extends Active((done.map(_._1) ++ todo).map(_.matchPlayer))

  case class WillCompleteTrick(players: List[(Player, VisibleCard)], deck: Deck, trump: VisibleCard)
      extends Active(players.map(_._1.matchPlayer))

  case class WillComplete(players: List[Player], trump: VisibleCard) extends Active(players.map(_.matchPlayer))

  case class WaitingForPlayer(ref: Int, timeout: Timeout.Active, request: Command.Act, state: PlayRound)
      extends Active(state.activePlayers)
      with Timer[BriscolaGameState, WaitingForPlayer]:
    override val timedOut: BriscolaGameState = BriscolaGameState.Aborted(GameAborted.Reason.playerTimeout)
    override def update(timeout: Timeout.Active, request: Command.Act): WaitingForPlayer =
      copy(timeout = timeout, request = request)

  sealed trait Terminated                          extends BriscolaGameState
  case class Completed(players: List[MatchPlayer]) extends Terminated
  case class Aborted(reason: GameAborted.Reason)   extends Terminated

  given encoder: Encoder[BriscolaGameState] = ConfiguredEncoder.derive(discriminator = Some("stage"))
  given decoder: Decoder[BriscolaGameState] = ConfiguredDecoder.derive(discriminator = Some("stage"))

end BriscolaGameState
