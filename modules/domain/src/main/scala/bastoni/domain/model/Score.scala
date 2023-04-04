package bastoni.domain.model

import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.Json.JObject

trait Score:
  def playerIds: List[UserId]
  def points: Int

extension [T](xs: List[T])
  def bestBy[U](f: T => U)(using ord: Ordering[U]): Option[T] =
    xs.map(x => x -> f(x)).sortBy(_._2)(using ord.reverse) match
      case (i1, v1) :: (i2, v2) :: _ if v1 != v2 => Some(i1)
      case (i, _) :: Nil                         => Some(i)
      case _                                     => None

extension (scores: List[Score]) def bestTeam: List[UserId] = scores.bestBy(_.points).map(_.playerIds).getOrElse(Nil)

case class GameScore(playerIds: List[UserId], points: Int, details: Json) extends Score

object GameScore:
  given Encoder[GameScore] = Encoder.instance(score =>
    Json.obj(
      "playerIds" -> score.playerIds.asJson,
      "points"    -> score.points.asJson,
      "details"   -> score.details
    )
  )

  given Decoder[GameScore] = Decoder.instance(cursor =>
    for
      playerIds <- cursor.downField("playerIds").as[List[UserId]]
      points    <- cursor.downField("points").as[Int]
      details <- cursor
        .downField("details")
        .focus
        .toRight(DecodingFailure("Details not found", cursor.downField("details").history))
    yield GameScore(playerIds, points, details)
  )

case class MatchScore(playerIds: List[UserId], points: Int) extends Score

object MatchScore:
  given Encoder[MatchScore] = deriveEncoder
  given Decoder[MatchScore] = deriveDecoder

  def forTeams(teams: List[List[MatchPlayer]]): List[MatchScore] =
    teams.map(players => MatchScore(players.map(_.id), players.headOption.fold(0)(_.points)))
