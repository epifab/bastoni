package bastoni.domain.logic.briscola

import bastoni.domain.logic.generic.Timer
import bastoni.domain.model.*
import bastoni.domain.model.Event.GameAborted
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait BriscolaGameState

object BriscolaGameState:
  sealed trait Active(val activePlayers: List[MatchPlayer]) extends BriscolaGameState
  case class   Ready(players: List[MatchPlayer]) extends Active(players)
  case class   DealRound(todo: List[Player], done: List[Player], deck: Deck) extends Active((done ++ todo).map(_.matchPlayer))
  case class   WillDealTrump(players: List[Player], deck: Deck) extends Active(players.map(_.matchPlayer))
  case class   WillPlay(round: PlayRound) extends Active(round.activePlayers)
  case class   DrawRound(todo: List[Player], done: List[Player], deck: Deck, trump: VisibleCard) extends Active((done ++ todo).map(_.matchPlayer))
  case class   PlayRound(todo: List[Player], done: List[(Player, VisibleCard)], deck: Deck, trump: VisibleCard) extends Active((done.map(_._1) ++ todo).map(_.matchPlayer))
  case class   WillCompleteTrick(players: List[(Player, VisibleCard)], deck: Deck, trump: VisibleCard) extends Active(players.map(_._1.matchPlayer))
  case class   WillComplete(players: List[Player], trump: VisibleCard) extends Active(players.map(_.matchPlayer))

  case class WaitingForPlayer(ref: Int, timeout: Timeout.Active, request: Command.Act, state: PlayRound) extends Active(state.activePlayers) with Timer[BriscolaGameState, WaitingForPlayer]:
    override val timedOut: BriscolaGameState = BriscolaGameState.Aborted(GameAborted.Reason.playerTimeout)
    override def update(timeout: Timeout.Active, request: Command.Act): WaitingForPlayer = copy(timeout = timeout, request = request)

  sealed trait Terminated extends BriscolaGameState
  case class   Completed(players: List[MatchPlayer]) extends Terminated
  case class   Aborted(reason: GameAborted.Reason) extends Terminated

  given encoder: Encoder[BriscolaGameState] = Encoder.instance {
    case s: Ready             => deriveEncoder[Ready].mapJsonObject(_.add("stage", "Ready".asJson))(s)
    case s: DealRound         => deriveEncoder[DealRound].mapJsonObject(_.add("stage", "DealRound".asJson))(s)
    case s: WillDealTrump     => deriveEncoder[WillDealTrump].mapJsonObject(_.add("stage", "WillDealTrump".asJson))(s)
    case s: WillPlay          => deriveEncoder[WillPlay].mapJsonObject(_.add("stage", "WillPlay".asJson))(s)
    case s: DrawRound         => deriveEncoder[DrawRound].mapJsonObject(_.add("stage", "DrawRound".asJson))(s)
    case s: PlayRound         => deriveEncoder[PlayRound].mapJsonObject(_.add("stage", "PlayRound".asJson))(s)
    case s: WillCompleteTrick => deriveEncoder[WillCompleteTrick].mapJsonObject(_.add("stage", "WillCompleteTrick".asJson))(s)
    case s: WaitingForPlayer  => deriveEncoder[WaitingForPlayer].mapJsonObject(_.add("stage", "WaitingForPlayer".asJson))(s)
    case s: WillComplete      => deriveEncoder[WillComplete].mapJsonObject(_.add("stage", "WillComplete".asJson))(s)
    case s: Completed         => deriveEncoder[Completed].mapJsonObject(_.add("stage", "Completed".asJson))(s)
    case s: Aborted           => deriveEncoder[Aborted].mapJsonObject(_.add("stage", "Aborted".asJson))(s)
  }

  given decoder: Decoder[BriscolaGameState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "Ready"             => deriveDecoder[Ready](cursor)
    case "DealRound"         => deriveDecoder[DealRound](cursor)
    case "WillDealTrump"     => deriveDecoder[WillDealTrump](cursor)
    case "WillPlay"          => deriveDecoder[WillPlay](cursor)
    case "DrawRound"         => deriveDecoder[DrawRound](cursor)
    case "PlayRound"         => deriveDecoder[PlayRound](cursor)
    case "WillCompleteTrick" => deriveDecoder[WillCompleteTrick](cursor)
    case "WaitingForPlayer"  => deriveDecoder[WaitingForPlayer](cursor)
    case "WillComplete"      => deriveDecoder[WillComplete](cursor)
    case "Completed"         => deriveDecoder[Completed](cursor)
    case "Aborted"           => deriveDecoder[Aborted](cursor)
  })
