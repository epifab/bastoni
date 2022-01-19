package bastoni.domain.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class MatchInfo(gameType: GameType, matchScore: List[MatchScore], gameScore: Option[List[GameScore]])

object MatchInfo:
  given Codec[MatchInfo] = deriveCodec
