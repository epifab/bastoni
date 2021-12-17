package bastoni.domain.model

import bastoni.domain.model.Rank.*
import bastoni.domain.model.Suit.*
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder, Json}

extension[T](xs: List[T])
  def bestBy[U](f: T => U)(using ord: Ordering[U]): Option[T] =
    xs.map(x => x -> f(x)).sortBy(_._2)(using ord.reverse) match
      case (i1, v1) :: (i2, v2) :: _ if v1 != v2 => Some(i1)
      case (i, _) :: Nil => Some(i)
      case _ => None


extension(scores: List[Score])
  def winners: List[UserId] = scores.bestBy(_.points).map(_.playerIds).getOrElse(Nil)

trait Score:
  def playerIds: List[UserId]
  def points: Int


case class MatchScore(playerIds: List[UserId], points: Int) extends Score

object MatchScore:
  given Encoder[MatchScore] = deriveEncoder
  given Decoder[MatchScore] = deriveDecoder
