package bastoni.domain.logic.tressette

import bastoni.domain.logic.ActiveMatch
import bastoni.domain.model.{MatchPlayer, ServerEvent, User}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}

sealed trait MatchState

object MatchState:
  def apply(users: List[User], pointsToWin: Int = 21): InProgress =
    val players = users.map(MatchPlayer(_, 0))
    InProgress(players, GameState.Ready(players), pointsToWin)

  case class  InProgress(players: List[MatchPlayer], game: GameState, pointsToWin: Int) extends MatchState with ActiveMatch
  case class  GameOver(event: ServerEvent, next: MatchState) extends MatchState
  case object Terminated extends MatchState

  given Encoder[MatchState] = Encoder.instance {
    case s: InProgress => deriveEncoder[InProgress].mapJsonObject(_.add("stage", "InProgress".asJson))(s)
    case s: GameOver => deriveEncoder[GameOver].mapJsonObject(_.add("stage", "GameOver".asJson))(s)
    case Terminated    => Json.obj("stage" -> "Terminated".asJson)
  }

  given Decoder[MatchState] = Decoder.instance(cursor => cursor.downField("stage").as[String].flatMap {
    case "InProgress" => deriveDecoder[InProgress](cursor)
    case "GameOver" => deriveDecoder[GameOver](cursor)
    case "Terminated" => Right(Terminated)
  })
