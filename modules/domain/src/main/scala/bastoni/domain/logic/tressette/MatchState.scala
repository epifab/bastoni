package bastoni.domain.logic.tressette

import bastoni.domain.model.{MatchPlayer, User}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Json, Decoder, Encoder}

sealed trait MatchState

object MatchState:
  def apply(users: List[User], pointsToWin: Int = 21): InProgress =
    val players = users.map(MatchPlayer(_, 0))
    InProgress(players, GameState.Ready(players), pointsToWin)

  case class  InProgress(players: List[MatchPlayer], state: GameState, pointsToWin: Int) extends MatchState
  case object Terminated extends MatchState

  given Encoder[MatchState] = Encoder.instance {
    case s: InProgress => deriveEncoder[InProgress].mapJsonObject(_.add("inProgress", true.asJson))(s)
    case Terminated    => Json.obj("inProgress" -> false.asJson)
  }

  given Decoder[MatchState] = Decoder.instance(cursor => cursor.downField("inProgress").as[Boolean].flatMap {
    case true => deriveDecoder[InProgress](cursor)
    case false => Right(Terminated)
  })
