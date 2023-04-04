package bastoni.domain.model

import io.circe.{Decoder, Encoder}

sealed trait Timeout(val value: Int)

object Timeout:
  case object TimedOut extends Timeout(0)

  sealed abstract class Active(value: Int, val next: Timeout) extends Timeout(value)

  case object T1  extends Active(1, TimedOut)
  case object T2  extends Active(2, T1)
  case object T3  extends Active(3, T2)
  case object T4  extends Active(4, T3)
  case object T5  extends Active(5, T4)
  case object T6  extends Active(6, T5)
  case object T7  extends Active(7, T6)
  case object T8  extends Active(8, T7)
  case object T9  extends Active(9, T8)
  case object Max extends Active(10, T9)

  object Active:
    given Encoder[Active] = Encoder[Int].contramap(_.value)

    given Decoder[Active] = Decoder[Int].emap {
      case 1  => Right(T1)
      case 2  => Right(T2)
      case 3  => Right(T3)
      case 4  => Right(T4)
      case 5  => Right(T5)
      case 6  => Right(T6)
      case 7  => Right(T7)
      case 8  => Right(T8)
      case 9  => Right(T9)
      case 10 => Right(Max)
      case e  => Left("Invalid timeout")
    }
end Timeout
