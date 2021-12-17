package bastoni.backend.briscola

import bastoni.domain.model.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait MatchState

object MatchState:
  def apply(players: List[Player]): Ready =
    Ready(players.map(GamePlayer(_, 0)))

  sealed trait Active(val activePlayers: List[GamePlayer]) extends MatchState
  case class   Ready(players: List[GamePlayer]) extends Active(players)
  case class   DealRound(todo: List[MatchPlayer], done: List[MatchPlayer], remaining: Int, deck: List[Card]) extends Active((done ++ todo).map(_.gamePlayer))
  case class   WillDealTrump(players: List[MatchPlayer], deck: List[Card]) extends Active(players.map(_.gamePlayer))
  case class   DrawRound(todo: List[MatchPlayer], done: List[MatchPlayer], deck: List[Card], trump: Card) extends Active((done ++ todo).map(_.gamePlayer))
  case class   PlayRound(todo: List[MatchPlayer], done: List[(MatchPlayer, Card)], deck: List[Card], trump: Card) extends Active((done.map(_._1) ++ todo).map(_.gamePlayer))
  case class   WillCompleteTrick(players: List[(MatchPlayer, Card)], deck: List[Card], trump: Card) extends Active(players.map(_._1.gamePlayer))
  case class   WillComplete(players: List[MatchPlayer], trump: Card) extends Active(players.map(_.gamePlayer))

  sealed trait Terminated extends MatchState
  case class   Completed(winners: List[PlayerId]) extends Terminated
  case object  Aborted extends Terminated

  given Encoder[MatchState] = Encoder.instance {
    case s: Ready             => deriveEncoder[Ready].mapJsonObject(_.add("stage", "Ready".asJson))(s)
    case s: DealRound         => deriveEncoder[DealRound].mapJsonObject(_.add("stage", "DealRound".asJson))(s)
    case s: WillDealTrump     => deriveEncoder[WillDealTrump].mapJsonObject(_.add("stage", "WillDealTrump".asJson))(s)
    case s: DrawRound         => deriveEncoder[DrawRound].mapJsonObject(_.add("stage", "DrawRound".asJson))(s)
    case s: PlayRound         => deriveEncoder[PlayRound].mapJsonObject(_.add("stage", "PlayRound".asJson))(s)
    case s: WillCompleteTrick => deriveEncoder[WillCompleteTrick].mapJsonObject(_.add("stage", "WillCompleteTrick".asJson))(s)
    case s: WillComplete      => deriveEncoder[WillComplete].mapJsonObject(_.add("stage", "WillComplete".asJson))(s)
    case s: Completed         => deriveEncoder[Completed].mapJsonObject(_.add("stage", "Completed".asJson))(s)
    case Aborted              => Json.obj("stage" -> "Aborted".asJson)
  }

  given Decoder[MatchState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "Ready"             => deriveDecoder[Ready](cursor)
    case "DealRound"         => deriveDecoder[DealRound](cursor)
    case "WillDealTrump"     => deriveDecoder[WillDealTrump](cursor)
    case "DrawRound"         => deriveDecoder[DrawRound](cursor)
    case "PlayRound"         => deriveDecoder[PlayRound](cursor)
    case "WillCompleteTrick" => deriveDecoder[WillCompleteTrick](cursor)
    case "WillComplete"      => deriveDecoder[WillComplete](cursor)
    case "Completed"         => deriveDecoder[Completed](cursor)
    case "Aborted"           => Right(Aborted)
  })
