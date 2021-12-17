package bastoni.domain.model

import bastoni.domain.model.Command.PlayCard
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

sealed trait Command

object Command:

  case object Connect extends Command
  case class  JoinTable(user: User, seed: Int) extends Command
  case class  LeaveTable(user: User) extends Command
  case class  StartGame(playerId: UserId, gameType: GameType) extends Command
  case class  ShuffleDeck(seed: Int) extends Command
  case class  PlayCard(playerId: UserId, card: Card) extends Command
  case class  TakeCards(playerId: UserId, played: Card, taken: List[Card]) extends Command
  case object Continue extends Command
  case class  Tick(ref: Int) extends Command

  given Encoder[Command] = Encoder.instance {
    case obj: JoinTable     => deriveEncoder[JoinTable].mapJsonObject(_.add("type", "JoinTable".asJson))(obj)
    case obj: LeaveTable    => deriveEncoder[LeaveTable].mapJsonObject(_.add("type", "LeaveRoom".asJson))(obj)
    case obj: StartGame     => deriveEncoder[StartGame].mapJsonObject(_.add("type", "StartGame".asJson))(obj)
    case obj: ShuffleDeck   => deriveEncoder[ShuffleDeck].mapJsonObject(_.add("type", "ShuffleDeck".asJson))(obj)
    case obj: PlayCard      => deriveEncoder[PlayCard].mapJsonObject(_.add("type", "PlayCard".asJson))(obj)
    case obj: TakeCards     => deriveEncoder[TakeCards].mapJsonObject(_.add("type", "TakeCards".asJson))(obj)
    case obj: Tick          => deriveEncoder[Tick].mapJsonObject(_.add("type", "Tick".asJson))(obj)
    case Connect            => Json.obj("type" -> "Connect".asJson)
    case Continue           => Json.obj("type" -> "Continue".asJson)
  }

  given Decoder[Command] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "JoinTable"     => deriveDecoder[JoinTable](obj)
    case "LeaveTable"    => deriveDecoder[LeaveTable](obj)
    case "StartGame"     => deriveDecoder[StartGame](obj)
    case "ShuffleDeck"   => deriveDecoder[ShuffleDeck](obj)
    case "PlayCard"      => deriveDecoder[PlayCard](obj)
    case "TakeCards"     => deriveDecoder[TakeCards](obj)
    case "Tick"          => deriveDecoder[Tick](obj)
    case "Connect"       => Right(Connect)
    case "Continue"      => Right(Continue)
  })
