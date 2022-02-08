package bastoni.domain.logic.tressette

import bastoni.domain.logic.generic.Timer
import bastoni.domain.logic.scopa.ScopaGameState
import bastoni.domain.logic.scopa.ScopaGameState.{Active, PlayRound}
import bastoni.domain.model.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait TressetteGameState

object TressetteGameState:
  sealed trait Active(val activePlayers: List[MatchPlayer]) extends TressetteGameState
  case class   Ready(players: List[MatchPlayer]) extends Active(players)
  case class   DealRound(todo: List[Player], done: List[Player], remaining: Int, deck: Deck) extends Active((done ++ todo).map(_.matchPlayer))
  case class   DrawRound(todo: List[Player], done: List[Player], deck: Deck) extends Active((done ++ todo).map(_.matchPlayer))
  case class   PlayRound(todo: List[Player], done: List[(Player, VisibleCard)], deck: Deck) extends Active((done.map(_._1) ++ todo).map(_.matchPlayer))
  case class   WillPlay(round: PlayRound) extends Active(round.activePlayers)
  case class   WillCompleteTrick(players: List[(Player, VisibleCard)], deck: Deck) extends Active(players.map(_._1.matchPlayer))
  case class   WillComplete(players: List[Player]) extends Active(players.map(_.matchPlayer))

  case class WaitingForPlayer(ref: Int, timeout: Timeout.Active, request: Command.Act, state: PlayRound) extends Active(state.activePlayers) with Timer[TressetteGameState, WaitingForPlayer]:
    override val timedOut: TressetteGameState = TressetteGameState.Aborted
    override def update(timeout: Timeout.Active, request: Command.Act): WaitingForPlayer = copy(timeout = timeout, request = request)

  sealed trait Terminated extends TressetteGameState
  case class   Completed(players: List[MatchPlayer]) extends Terminated
  case object  Aborted extends Terminated

  given encoder: Encoder[TressetteGameState] = Encoder.instance {
    case s: Ready             => deriveEncoder[Ready].mapJsonObject(_.add("stage", "Ready".asJson))(s)
    case s: DealRound         => deriveEncoder[DealRound].mapJsonObject(_.add("stage", "DealRound".asJson))(s)
    case s: DrawRound         => deriveEncoder[DrawRound].mapJsonObject(_.add("stage", "DrawRound".asJson))(s)
    case s: PlayRound         => deriveEncoder[PlayRound].mapJsonObject(_.add("stage", "PlayRound".asJson))(s)
    case s: WaitingForPlayer  => deriveEncoder[WaitingForPlayer].mapJsonObject(_.add("stage", "WaitingForPlayer".asJson))(s)
    case s: WillPlay          => deriveEncoder[WillPlay].mapJsonObject(_.add("stage", "WillPlay".asJson))(s)
    case s: WillCompleteTrick => deriveEncoder[WillCompleteTrick].mapJsonObject(_.add("stage", "WillCompleteTrick".asJson))(s)
    case s: WillComplete      => deriveEncoder[WillComplete].mapJsonObject(_.add("stage", "WillComplete".asJson))(s)
    case s: Completed         => deriveEncoder[Completed].mapJsonObject(_.add("stage", "Completed".asJson))(s)
    case Aborted              => Json.obj("stage" -> "Aborted".asJson)
  }

  given decoder: Decoder[TressetteGameState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "Ready"             => deriveDecoder[Ready](cursor)
    case "DealRound"         => deriveDecoder[DealRound](cursor)
    case "DrawRound"         => deriveDecoder[DrawRound](cursor)
    case "PlayRound"         => deriveDecoder[PlayRound](cursor)
    case "WaitingForPlayer"  => deriveDecoder[WaitingForPlayer](cursor)
    case "WillPlay"          => deriveDecoder[WillPlay](cursor)
    case "WillCompleteTrick" => deriveDecoder[WillCompleteTrick](cursor)
    case "WillComplete"      => deriveDecoder[WillComplete](cursor)
    case "Completed"         => deriveDecoder[Completed](cursor)
    case "Aborted"           => Right(Aborted)
  })
