package bastoni.domain.model

import io.circe.{Decoder, Encoder, Codec}
import io.circe.generic.semiauto.deriveCodec

enum Delay:
  case Short, Medium, Long, Tick

object Delay:
  given Encoder[Delay] = Encoder[String].contramap(_.toString)
  given Decoder[Delay] = Decoder[String].map(Delay.valueOf)

case class Delayed[+T](inner: T, delay: Delay):
  def map[U](f: T => U): Delayed[U] = Delayed(f(inner), delay)

object Delayed:
  given [T](using Codec[T]): Codec[Delayed[T]] = deriveCodec

extension (command: Command)
  def shortly: Delayed[Command]   = Delayed(command, Delay.Short)
  def later: Delayed[Command]     = Delayed(command, Delay.Medium)
  def muchLater: Delayed[Command] = Delayed(command, Delay.Long)

type PotentiallyDelayed[T] = T | Delayed[T]

object PotentiallyDelayed:
  given decoder[T](using Decoder[T]): Decoder[T | Delayed[T]] = Decoder[(T, Option[Delay])].map {
    case (t, Some(delay)) => Delayed(t, delay)
    case (t, None)        => t
  }

  given messageEncoder: Encoder[Message | Delayed[Message]] = Encoder[(Message, Option[Delay])].contramap {
    case Delayed(t: Message, delay) => t -> Some(delay)
    case t: Message                 => t -> None
  }
