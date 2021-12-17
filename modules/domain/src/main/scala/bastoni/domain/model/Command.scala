package bastoni.domain.model

import bastoni.domain.model.Command.PlayCard
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

sealed trait Command

object Command:

  case object Connect extends Command
  case class  JoinTable(player: Player, seed: Int) extends Command
  case class  LeaveTable(player: Player) extends Command
  case class  StartGame(player: PlayerId, gameType: GameType) extends Command
  case class  ShuffleDeck(seed: Int) extends Command
  case class  PlayCard(player: PlayerId, card: Card) extends Command
  case object Continue extends Command

  given Encoder[Command] = Encoder.instance {
    case Connect            => Json.obj("type" -> "Connect".asJson)
    case obj: JoinTable     => deriveEncoder[JoinTable].mapJsonObject(_.add("type", "JoinTable".asJson))(obj)
    case obj: LeaveTable    => deriveEncoder[LeaveTable].mapJsonObject(_.add("type", "LeaveRoom".asJson))(obj)
    case obj: StartGame     => deriveEncoder[StartGame].mapJsonObject(_.add("type", "StartGame".asJson))(obj)
    case obj: ShuffleDeck   => deriveEncoder[ShuffleDeck].mapJsonObject(_.add("type", "ShuffleDeck".asJson))(obj)
    case obj: PlayCard      => deriveEncoder[PlayCard].mapJsonObject(_.add("type", "PlayCard".asJson))(obj)
    case Continue           => Json.obj("type" -> "Continue".asJson)
  }

  given Decoder[Command] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    case "Connect"       => Right(Connect)
    case "JoinTable"     => deriveDecoder[JoinTable](obj)
    case "LeaveTable"    => deriveDecoder[LeaveTable](obj)
    case "StartGame"     => deriveDecoder[StartGame](obj)
    case "ShuffleDeck"   => deriveDecoder[ShuffleDeck](obj)
    case "PlayCard"      => deriveDecoder[PlayCard](obj)
    case "Continue"      => Right(Continue)
  })
