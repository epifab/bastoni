package bastoni.domain.logic.tressette

import bastoni.domain.model.{GamePlayer, Player}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Json, Decoder, Encoder}

sealed trait GameState

object GameState:
  def apply(players: List[Player], pointsToWin: Int = 21): InProgress =
    val gamePlayers = players.map(GamePlayer(_, 0))
    InProgress(gamePlayers, MatchState.Ready(gamePlayers), pointsToWin)

  case class  InProgress(players: List[GamePlayer], matchState: MatchState, pointsToWin: Int) extends GameState
  case object Terminated extends GameState

  given Encoder[GameState] = Encoder.instance {
    case s: InProgress => deriveEncoder[InProgress].mapJsonObject(_.add("inProgress", true.asJson))(s)
    case Terminated    => Json.obj("inProgress" -> false.asJson)
  }

  given Decoder[GameState] = Decoder.instance(cursor => cursor.downField("inProgress").as[Boolean].flatMap {
    case true => deriveDecoder[InProgress](cursor)
    case false => Right(Terminated)
  })