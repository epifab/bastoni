package bastoni.domain.model

import io.circe.{Encoder, Decoder}

enum GameType:
  case Briscola
  case Tressette

object GameType:
  given Encoder[GameType] = Encoder[String].contramap(_.toString)
  given Decoder[GameType] = Decoder[String].map(GameType.valueOf)
