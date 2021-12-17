package bastoni.domain.model

import io.circe.{Decoder, Encoder}

enum Direction:
  case Player, Up, Down

object Direction:
  given Encoder[Direction] = Encoder[String].contramap(_.toString)
  given Decoder[Direction] = Decoder[String].map(Direction.valueOf)
