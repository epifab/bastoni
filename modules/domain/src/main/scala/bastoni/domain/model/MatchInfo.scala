package bastoni.domain.model

import io.circe.generic.semiauto.deriveCodec
import io.circe.Codec

case class MatchInfo(gameType: GameType, matchScore: List[MatchScore], gameScore: Option[List[GameScore]])

object MatchInfo:
  given Codec[MatchInfo] = deriveCodec
