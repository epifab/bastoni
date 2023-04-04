package bastoni.domain.model

import io.circe.{Decoder, Encoder}

enum GameType:
  case Briscola
  case Tressette
  case Scopa

object GameType:
  given Encoder[GameType] = Encoder[String].contramap(_.toString)
  given Decoder[GameType] = Decoder[String].map(GameType.valueOf)
