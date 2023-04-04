package bastoni.domain.logic
package scopa

import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Event.{GameAborted, TimedOut}
import io.circe.{Decoder, Encoder, Json}
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

  given encoder: Encoder[ScopaGameState] = Encoder.instance {
    case s: Ready      => deriveEncoder[Ready].mapJsonObject(_.add("stage", "Ready".asJson))(s)
    case s: Deal3Round => deriveEncoder[Deal3Round].mapJsonObject(_.add("stage", "Deal3Round".asJson))(s)
    case s: Deal5Round => deriveEncoder[Deal5Round].mapJsonObject(_.add("stage", "Deal5Round".asJson))(s)
    case s: WillDealBoardCards =>
      deriveEncoder[WillDealBoardCards].mapJsonObject(_.add("stage", "WillDealBoardCards".asJson))(s)
    case s: WillPlay      => deriveEncoder[WillPlay].mapJsonObject(_.add("stage", "WillPlay".asJson))(s)
    case s: DrawRound     => deriveEncoder[DrawRound].mapJsonObject(_.add("stage", "DrawRound".asJson))(s)
    case s: PlayRound     => deriveEncoder[PlayRound].mapJsonObject(_.add("stage", "PlayRound".asJson))(s)
    case s: WillTakeCards => deriveEncoder[WillTakeCards].mapJsonObject(_.add("stage", "WillTakeCards".asJson))(s)
    case s: WaitingForPlayer =>
      deriveEncoder[WaitingForPlayer].mapJsonObject(_.add("stage", "WaitingForPlayer".asJson))(s)
    case s: WillComplete => deriveEncoder[WillComplete].mapJsonObject(_.add("stage", "WillComplete".asJson))(s)
    case s: Completed    => deriveEncoder[Completed].mapJsonObject(_.add("stage", "Completed".asJson))(s)
    case s: Aborted      => deriveEncoder[Aborted].mapJsonObject(_.add("stage", "Aborted".asJson))(s)
  }

  given decoder: Decoder[ScopaGameState] =
    Decoder
      .instance(cursor =>
        cursor
          .downField("stage")
          .as[String]
          .flatMap {
            case "Ready"              => deriveDecoder[Ready](cursor)
            case "Deal3Round"         => deriveDecoder[Deal3Round](cursor)
            case "Deal5Round"         => deriveDecoder[Deal5Round](cursor)
            case "WillDealBoardCards" => deriveDecoder[WillDealBoardCards](cursor)
            case "WillPlay"           => deriveDecoder[WillPlay](cursor)
            case "DrawRound"          => deriveDecoder[DrawRound](cursor)
            case "PlayRound"          => deriveDecoder[PlayRound](cursor)
            case "WillTakeCards"      => deriveDecoder[WillTakeCards](cursor)
            case "WaitingForPlayer"   => deriveDecoder[WaitingForPlayer](cursor)
            case "WillComplete"       => deriveDecoder[WillComplete](cursor)
            case "Completed"          => deriveDecoder[Completed](cursor)
            case "Aborted"            => deriveDecoder[Aborted](cursor)
          }
      )
end ScopaGameState
