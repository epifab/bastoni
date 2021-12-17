package bastoni.domain.logic
package scopa

import bastoni.domain.model.{MatchPlayer, User}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Json, Decoder, Encoder}

sealed trait MatchState

object MatchState:
  def apply(users: List[User]): InProgress =
    val players = users.map(MatchPlayer(_, 0))
    InProgress(players, GameState.Ready(players), if (players.size == 4) 21 else 11)

  case class  InProgress(players: List[MatchPlayer], game: GameState, pointsToWin: Int) extends MatchState
  case object Terminated extends MatchState

  given Encoder[MatchState] = Encoder.instance {
    case s: InProgress => deriveEncoder[InProgress].mapJsonObject(_.add("inProgress", true.asJson))(s)
    case Terminated    => Json.obj("inProgress" -> false.asJson)
  }

  given Decoder[MatchState] = Decoder.instance(cursor => cursor.downField("inProgress").as[Boolean].flatMap {
    case true => deriveDecoder[InProgress](cursor)
    case false => Right(Terminated)
  })
