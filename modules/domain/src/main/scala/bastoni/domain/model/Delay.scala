package bastoni.domain.model

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

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
  case class Delayable[T](item: T, delay: Option[Delay])

  given dc[T](using Decoder[T]): Decoder[Delayable[T]] = deriveDecoder
  given ec[T](using Encoder[T]): Encoder[Delayable[T]] = deriveEncoder

  given decoder[T](using Decoder[T]): Decoder[T | Delayed[T]] = Decoder[Delayable[T]].map {
    case Delayable(t, Some(delay)) => Delayed(t, delay)
    case Delayable(t, None)        => t
  }

  given messageEncoder: Encoder[Message | Delayed[Message]] = Encoder[Delayable[Message]].contramap {
    case Delayed(t: Message, delay) => Delayable(t, Some(delay))
    case t: Message                 => Delayable(t, None)
  }

  given commandEncoder: Encoder[Command | Delayed[Command]] = Encoder[Delayable[Command]].contramap {
    case Delayed(t: Command, delay) => Delayable(t, Some(delay))
    case t: Command                 => Delayable(t, None)
  }
