package bastoni.domain.logic
package scopa

import bastoni.domain.model.*
import bastoni.domain.model.Event.{ActionRequested, MatchAborted, TimedOut}
import bastoni.domain.logic.generic.Timer
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait MatchState

object MatchState:
  def apply(players: List[Player]): Ready =
    Ready(players.map(GamePlayer(_, 0)))

  sealed trait Active(val activePlayers: List[GamePlayer]) extends MatchState
  case class   Ready(players: List[GamePlayer]) extends Active(players)
  case class   DealRound(size: Int, todo: List[MatchPlayer], done: List[MatchPlayer], deck: List[Card], remaining: Int) extends Active((done ++ todo).map(_.gamePlayer))
  case class   WillDealBoardCards(players: List[MatchPlayer], deck: List[Card]) extends Active(players.map(_.gamePlayer))
  case class   DrawRound(todo: List[MatchPlayer], done: List[MatchPlayer], deck: List[Card], boardCards: List[Card]) extends Active((done ++ todo).map(_.gamePlayer))
  case class   PlayRound(players: List[MatchPlayer], deck: List[Card], board: List[Card]) extends Active(players.map(_.gamePlayer))
  case class   WillComplete(players: List[MatchPlayer]) extends Active(players.map(_.gamePlayer))

  case class WaitingForPlayer(ref: Int, timeout: Timeout.Active, request: ActionRequested, state: PlayRound) extends Active(state.activePlayers) with Timer[MatchState, WaitingForPlayer]:
    override val timedOut: MatchState = MatchState.Aborted
    override def update(timeout: Timeout.Active, request: ActionRequested): WaitingForPlayer = copy(timeout = timeout, request = request)

  sealed trait Terminated extends MatchState
  case class   Completed(players: List[GamePlayer]) extends Terminated
  case object  Aborted extends Terminated

  given Encoder[MatchState] = Encoder.instance {
    case s: Ready              => deriveEncoder[Ready].mapJsonObject(_.add("stage", "Ready".asJson))(s)
    case s: DealRound          => deriveEncoder[DealRound].mapJsonObject(_.add("stage", "DealRound".asJson))(s)
    case s: WillDealBoardCards => deriveEncoder[WillDealBoardCards].mapJsonObject(_.add("stage", "WillDealBoardCards".asJson))(s)
    case s: DrawRound          => deriveEncoder[DrawRound].mapJsonObject(_.add("stage", "DrawRound".asJson))(s)
    case s: PlayRound          => deriveEncoder[PlayRound].mapJsonObject(_.add("stage", "PlayRound".asJson))(s)
    case s: WaitingForPlayer   => deriveEncoder[WaitingForPlayer].mapJsonObject(_.add("stage", "WaitingForPlayer".asJson))(s)
    case s: WillComplete       => deriveEncoder[WillComplete].mapJsonObject(_.add("stage", "WillComplete".asJson))(s)
    case s: Completed          => deriveEncoder[Completed].mapJsonObject(_.add("stage", "Completed".asJson))(s)
    case Aborted               => Json.obj("stage" -> "Aborted".asJson)
  }

  given Decoder[MatchState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "Ready"              => deriveDecoder[Ready](cursor)
    case "DealRound"          => deriveDecoder[DealRound](cursor)
    case "WillDealBoardCards" => deriveDecoder[WillDealBoardCards](cursor)
    case "DrawRound"          => deriveDecoder[DrawRound](cursor)
    case "PlayRound"          => deriveDecoder[PlayRound](cursor)
    case "WaitingForPlayer"   => deriveDecoder[WaitingForPlayer](cursor)
    case "WillComplete"       => deriveDecoder[WillComplete](cursor)
    case "Completed"          => deriveDecoder[Completed](cursor)
    case "Aborted"            => Right(Aborted)
  })
