package bastoni.domain.model

import bastoni.domain.model.Command.PlayCard
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*

sealed trait Command

object Command:

  enum Action:
    case PlayCard
    case PlayCardOf(suit: Suit)
    case ShuffleDeck

  object Action:
    given Encoder[Action] = Encoder.instance {
      case PlayCard         => Json.obj("type" -> "PlayCard".asJson)
      case PlayCardOf(suit) => Json.obj("type" -> "PlayCardOf".asJson, "suit" -> suit.asJson)
      case ShuffleDeck      => Json.obj("type" -> "ShuffleDeck".asJson)
    }

    given Decoder[Action] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
      case "PlayCard"    => Right(PlayCard)
      case "PlayCardOf"  => obj.downField("suit").as[Suit].map(suit => PlayCardOf(suit))
      case "ShuffleDeck" => Right(ShuffleDeck)
    })

  // case object Observe extends Command
  case class  JoinRoom(player: Player) extends Command
  case class  LeaveRoom(player: Player) extends Command
  case class  ActivateRoom(player: Player, gameType: GameType) extends Command
  case class  StartGame(room: Room, gameType: GameType) extends Command
  case class  ShuffleDeck(seed: Int) extends Command
  case class  PlayCard(player: PlayerId, card: Card) extends Command
  case class  ActionRequest(playerId: PlayerId, action: Action) extends Command
  case object Continue extends Command

  given Encoder[Command] = Encoder.instance {
    // case Observe            => Json.obj("type" -> "Observe".asJson)
    case obj: JoinRoom      => deriveEncoder[JoinRoom].mapJsonObject(_.add("type", "JoinRoom".asJson))(obj)
    case obj: LeaveRoom     => deriveEncoder[LeaveRoom].mapJsonObject(_.add("type", "LeaveRoom".asJson))(obj)
    case obj: ActivateRoom  => deriveEncoder[ActivateRoom].mapJsonObject(_.add("type", "ActivateRoom".asJson))(obj)
    case obj: StartGame     => deriveEncoder[StartGame].mapJsonObject(_.add("type", "StartGame".asJson))(obj)
    case obj: ShuffleDeck   => deriveEncoder[ShuffleDeck].mapJsonObject(_.add("type", "ShuffleDeck".asJson))(obj)
    case obj: PlayCard      => deriveEncoder[PlayCard].mapJsonObject(_.add("type", "PlayCard".asJson))(obj)
    case obj: ActionRequest => deriveEncoder[ActionRequest].mapJsonObject(_.add("type", "ActionRequest".asJson))(obj)
    case Continue           => Json.obj("type" -> "Continue".asJson)
  }

  given Decoder[Command] = Decoder.instance(obj => obj.downField("type").as[String].flatMap {
    // case "Observe"       => Right(Observe)
    case "JoinRoom"      => deriveDecoder[JoinRoom](obj)
    case "LeaveRoom"     => deriveDecoder[LeaveRoom](obj)
    case "ActivateRoom"  => deriveDecoder[ActivateRoom](obj)
    case "StartGame"     => deriveDecoder[StartGame](obj)
    case "ShuffleDeck"   => deriveDecoder[ShuffleDeck](obj)
    case "PlayCard"      => deriveDecoder[PlayCard](obj)
    case "ActionRequest" => deriveDecoder[ActionRequest](obj)
    case "Continue"      => Right(Continue)
  })
