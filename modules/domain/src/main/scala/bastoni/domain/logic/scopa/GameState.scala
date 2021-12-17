package bastoni.domain.logic
package scopa

import bastoni.domain.model.*
import bastoni.domain.model.Event.{ActionRequested, GameAborted, TimedOut}
import bastoni.domain.logic.generic.Timer
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait GameState

object GameState:
  def apply(users: List[User]): Ready =
    Ready(users.map(MatchPlayer(_, 0)))

  sealed trait Active(val activePlayers: List[MatchPlayer]) extends GameState
  case class   Ready(players: List[MatchPlayer]) extends Active(players)
  case class   DealRound(size: Int, todo: List[Player], done: List[Player], deck: List[Card], remaining: Int) extends Active((done ++ todo).map(_.matchPlayer))
  case class   WillDealBoardCards(players: List[Player], deck: List[Card]) extends Active(players.map(_.matchPlayer))
  case class   DrawRound(todo: List[Player], done: List[Player], deck: List[Card], boardCards: List[Card]) extends Active((done ++ todo).map(_.matchPlayer))
  case class   PlayRound(players: List[Player], deck: List[Card], board: List[Card]) extends Active(players.map(_.matchPlayer))
  case class   WillComplete(players: List[Player]) extends Active(players.map(_.matchPlayer))

  case class WaitingForPlayer(ref: Int, timeout: Timeout.Active, request: ActionRequested, state: PlayRound) extends Active(state.activePlayers) with Timer[GameState, WaitingForPlayer]:
    override val timedOut: GameState = GameState.Aborted
    override def update(timeout: Timeout.Active, request: ActionRequested): WaitingForPlayer = copy(timeout = timeout, request = request)

  sealed trait Terminated extends GameState
  case class   Completed(players: List[MatchPlayer]) extends Terminated
  case object  Aborted extends Terminated

  given Encoder[GameState] = Encoder.instance {
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

  given Decoder[GameState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
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